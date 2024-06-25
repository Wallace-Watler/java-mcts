package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Closed loop MCTS with root parallelization and transposition table.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MCTS
 * @see MCTSRP
 * @see MCTSTP
 */
public final class MCTSRPT implements MCTS {
    @Override
    public
    <STATE extends VisibleState<STATE, ? extends ACTION>, ACTION extends Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(rootState.validActions().isEmpty())
            return new SearchResults<>(null, 0, 0);

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger totalIters = new AtomicInteger();
        // -------------------------------

        // One search tree for each thread
        final ArrayList<Node<STATE, ACTION>> rootNodes = new ArrayList<>(params.threadCount());
        for(int i = 0; i < params.threadCount(); i++)
            rootNodes.add(new Node<>(numPlayers, rootState, null));

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            final Node<STATE, ACTION> rootNode = rootNodes.get(workerNum);
            workers[workerNum] = new Thread(() -> totalIters.getAndAdd(rootParallelSearch(rootNode, params, rand, start)), "mctsrp" + workerNum);
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
        for(Node<STATE, ACTION> node : rootNodes) {
            final ACTION action = SearchNode.mostVisited(node, node.validActions, rand);
            votes.put(action, votes.getOrDefault(action, 0) + 1);
        }
        final ACTION bestAction = votes.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
        final double itersPerThread = (double) totalIters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start);
    }

    private static
    <STATE extends VisibleState<STATE, ? extends ACTION>, ACTION extends Action<STATE>>
    int rootParallelSearch(Node<STATE, ACTION> rootNode, SearchParameters params, Random rand, long start) {
        long now = System.currentTimeMillis();

        int iters = 0;
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters < params.maxIters())) {
            iters++;

            Node<STATE, ACTION> currentNode = rootNode;

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(currentNode.scores == null && continueSelection) {
                final ACTION selectedAction = params.uct().selectBranch(currentNode, rand, currentNode.state.activePlayer(), currentNode.validActions);
                currentNode.createChild(selectedAction);
                final Node<STATE, ACTION> selectedChild = currentNode.getChild(selectedAction);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                currentNode = selectedChild;
            }

            // Simulation - Choose a random action until the game is decided.
            STATE simulatedState = currentNode.state.copy();
            while(simulatedState.scores() == null) {
                final List<? extends ACTION> validActions = simulatedState.validActions();
                final ACTION action = validActions.get(rand.nextInt(validActions.size()));
                simulatedState = action.applyToState(simulatedState);
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            currentNode.backPropagate(simulatedState.scores());

            now = System.currentTimeMillis();
        }
        return iters;
    }

    /**
     * A node in an MCTS-RP search tree. The tree is structured such that each node stores a game state, and each action
     * leading from a node maps to a unique child node.
     */
    private static final class Node<STATE extends VisibleState<STATE, ? extends ACTION>, ACTION extends Action<STATE>> implements SearchNode<ACTION> {
        private final Node<STATE, ACTION> parent;
        private final HashMap<ACTION, Node<STATE, ACTION>> children;
        private final STATE state;

        /** Cached {@code state.validActions()}. */
        private final List<? extends ACTION> validActions;

        /** Cached {@code state.scores()}. */
        private final double[] scores;

        /** Number of times this node has been visited. */
        private int visitCount = 0;

        /** Number of times each action has been taken from this node. */
        private final HashMap<ACTION, Integer> actionCounts;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        private Node(int numPlayers, STATE state, Node<STATE, ACTION> parent) {
            this.parent = parent;
            this.state = state;
            validActions = state.validActions();
            scores = state.scores();
            totalScores = new double[numPlayers];

            children = new HashMap<>(validActions.size());
            actionCounts = new HashMap<>(validActions.size());
        }

        @Override
        public Node<STATE, ACTION> getChild(ACTION action) {
            return children.get(action);
        }

        @Override
        public void createChild(ACTION action) {
            if(children.get(action) == null) {
                final STATE state = action.applyToState(this.state.copy());
                children.put(action, new Node<>(totalScores.length, state, this));
            }
        }

        @Override
        public int visitCount() {
            return visitCount;
        }

        @Override
        public int availableCount() {
            return parent.visitCount;
        }

        @Override
        public double totalScore(int activePlayer) {
            return totalScores[activePlayer];
        }

        @Override
        public ReadWriteLock statsLock() {
            return DUMMY_LOCK;
        }

        private void backPropagate(double[] scores) {
            visitCount++;
            for(int i = 0; i < scores.length; i++)
                totalScores[i] += scores[i];

            if(parent != null)
                parent.backPropagate(scores);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "visitCount=" + visitCount +
                    ", totalReward=" + Arrays.toString(totalScores) +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MCTS-RP-T";
    }
}
