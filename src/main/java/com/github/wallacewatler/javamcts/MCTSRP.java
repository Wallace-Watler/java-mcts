package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Closed loop MCTS with root parallelization.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MCTS
 * @see MCTSTP
 */
public final class MCTSRP implements MCTS {
    @Override
    public
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand, boolean useTTable) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(rootState.validActions().isEmpty())
            return new SearchResults<>(null, 0, 0, 1, 1);

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger totalIters = new AtomicInteger();
        final AtomicInteger numStates = new AtomicInteger();
        // -------------------------------

        // One search tree for each thread
        final ArrayList<Node<STATE, ACTION>> rootNodes = new ArrayList<>(params.threadCount());
        for(int i = 0; i < params.threadCount(); i++)
            rootNodes.add(new Node<>(numPlayers, rootState));

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            final Node<STATE, ACTION> rootNode = rootNodes.get(workerNum);
            workers[workerNum] = new Thread(() -> totalIters.getAndAdd(rootParallelSearch(rootNode, params, rand, useTTable, start, numStates)), "mctsrp" + workerNum);
            workers[workerNum].start();
        }

        // Wait for all threads to finish
        try {
            for(Thread worker : workers)
                worker.join();
        } catch(InterruptedException ignored) {
            for(Thread worker : workers)
                worker.interrupt();
        }

        // Recommend the most selected action by majority voting.
        final HashMap<ACTION, Integer> votes = new HashMap<>();
        int numNodes = 0;
        for(Node<STATE, ACTION> root : rootNodes) {
            final ACTION action = SearchNode.mostVisited(root, root.validActions(), rand);
            votes.put(action, votes.getOrDefault(action, 0) + 1);
            numNodes += root.numNodes();
        }
        final ACTION bestAction = votes.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
        final double itersPerThread = (double) totalIters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start, numNodes, numStates.get());
    }

    private static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>>
    int rootParallelSearch(Node<STATE, ACTION> rootNode, SearchParameters params, Random rand, boolean useTTable, long start, AtomicInteger numStates) {
        long now = System.currentTimeMillis();

        final ArrayDeque<Node<STATE, ACTION>> nodePath = new ArrayDeque<>();
        final TranspositionTable<STATE, Node<STATE, ACTION>> transpositionTable = useTTable ? new RealTTable<>() : new DummyTTable<>();

        int iters = 0;
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters < params.maxIters())) {
            iters++;

            Node<STATE, ACTION> currentNode = rootNode;
            nodePath.add(currentNode);

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(currentNode.scores() == null && continueSelection) {
                final ACTION selectedAction = SearchNode.selectBranch(currentNode, currentNode.validActions(), currentNode.state.activePlayer(), params.uct(), rand);
                currentNode.createChildIfNotPresent(selectedAction, transpositionTable);
                final Node<STATE, ACTION> selectedChild = currentNode.getChild(selectedAction);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                currentNode = selectedChild;
                nodePath.add(currentNode);
            }

            // Simulation - Choose a random action until the game is decided.
            STATE simulatedState = currentNode.state.copy();
            while(simulatedState.scores() == null) {
                final List<ACTION> validActions = simulatedState.validActions();
                final ACTION action = validActions.get(rand.nextInt(validActions.size()));
                simulatedState = action.applyToState(simulatedState);
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            final double[] scores = simulatedState.scores();
            while(!nodePath.isEmpty()) {
                final Node<STATE, ACTION> node = nodePath.removeLast();
                node.visitCount++;
                for(int i = 0; i < scores.length; i++)
                    node.totalScores[i] += scores[i];
            }

            now = System.currentTimeMillis();
        }
        numStates.getAndAdd(transpositionTable.size());
        return iters;
    }

    @Override
    public String toString() {
        return "MCTS-RP";
    }

    /**
     * A node in an MCTS-RP search tree. The tree is structured such that each node stores a game state, and each action
     * leading from a node maps to a unique child node.
     */
    private static final class Node<STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>> implements SearchNode<ACTION> {
        private final HashMap<ACTION, Node<STATE, ACTION>> children;
        private final STATE state;

        /** Number of times this node has been visited. */
        private int visitCount = 0;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        public Node(int numPlayers, STATE state) {
            this.state = state;
            totalScores = new double[numPlayers];
            children = new HashMap<>();
        }

        @Override
        public int visitCount() {
            return visitCount;
        }

        @Override
        public double totalScore(int activePlayer) {
            return totalScores[activePlayer];
        }

        @Override
        public Node<STATE, ACTION> getChild(ACTION action) {
            return children.get(action);
        }

        @Override
        public int selectCount(ACTION action) {
            return getChild(action).visitCount;
        }

        @Override
        public int availableCount(ACTION action) {
            return visitCount;
        }

        @Override
        public ReadWriteLock statsLock() {
            return DUMMY_LOCK;
        }

        public void createChildIfNotPresent(ACTION action, TranspositionTable<STATE, Node<STATE, ACTION>> transpositionTable) {
            if(children.get(action) == null) {
                final STATE state = action.applyToState(this.state.copy());
                if(transpositionTable.contains(state)) {
                    children.put(action, transpositionTable.get(state));
                } else {
                    final Node<STATE, ACTION> child = new Node<>(totalScores.length, state);
                    children.put(action, child);
                    transpositionTable.put(state, child);
                }
            }
        }

        public List<ACTION> validActions() {
            return state.validActions();
        }

        public double[] scores() {
            return state.scores();
        }

        public int numNodes() {
            int num = 1;
            for(Node<STATE, ACTION> child : children.values())
                num += child.numNodes();

            return num;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "visitCount=" + visitCount +
                    ", totalReward=" + Arrays.toString(totalScores) +
                    '}';
        }
    }
}
