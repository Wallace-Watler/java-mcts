package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Open-loop MCTS with root-parallelization.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see OLMCTSTP
 */
public final class OLMCTSRP implements OLMCTS {
    @Override
    public
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends OLMCTS.Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        final List<ACTION> validActions = rootState.validActions();

        if(validActions.isEmpty())
            throw new IllegalArgumentException("rootState has no valid actions");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger totalIters = new AtomicInteger();
        // -------------------------------

        // One search tree for each thread
        final ArrayList<Node> rootNodes = new ArrayList<>(params.threadCount());
        for(int i = 0; i < params.threadCount(); i++)
            rootNodes.add(new Node(numPlayers, null));

        // Start parallel searches. The main thread does one of them.
        final Thread[] workers = new Thread[params.threadCount() - 1];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            final Node rootNode = rootNodes.get(workerNum + 1);
            workers[workerNum] = new Thread(() -> totalIters.addAndGet(rootParallelSearch(rootState, rootNode, params, rand, start)), "olmctsrp" + workerNum);
            workers[workerNum].start();
        }
        totalIters.addAndGet(rootParallelSearch(rootState, rootNodes.get(0), params, rand, start));

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
        for(Node node : rootNodes) {
            final ACTION action = SearchNode.mostVisited(node, validActions, rand);
            votes.put(action, votes.getOrDefault(action, 0) + 1);
        }
        final ACTION bestAction = votes.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
        final double itersPerThread = (double) totalIters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start);
    }

    private static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends OLMCTS.Action<STATE>>
    int rootParallelSearch(STATE rootState, Node rootNode, SearchParameters params, Random rand, long start) {
        long now = System.currentTimeMillis();

        int iters = 0;
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters < params.maxIters())) {
            iters++;

            Node currentNode = rootNode;
            STATE simulatedState = rootState.copy();
            List<ACTION> validActions = simulatedState.validActions();

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(simulatedState.scores() == null && continueSelection) {
                final int activePlayer = simulatedState.activePlayer();
                for(ACTION action : validActions)
                    currentNode.createChild(action);

                final ACTION selectedAction = params.uct().selectBranch(currentNode, rand, activePlayer, validActions);
                final Node selectedChild = currentNode.getChild(selectedAction);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                for(ACTION action : validActions)
                    currentNode.getChild(action).availableCount++;

                simulatedState = selectedAction.applyToState(simulatedState, rand);
                validActions = simulatedState.validActions();

                currentNode = selectedChild;
            }

            // Simulation - Choose a random action until the game is decided.
            while(simulatedState.scores() == null) {
                final ACTION action = validActions.get(rand.nextInt(validActions.size()));
                simulatedState = action.applyToState(simulatedState, rand);
                validActions = simulatedState.validActions();
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            currentNode.backPropagate(simulatedState.scores());

            now = System.currentTimeMillis();
        }
        return iters;
    }

    /**
     * A node in an OLMCTS-RP search tree. Since actions are stochastic and states may be continuous in OLMCTS, a single
     * node represents a distribution of states reached via a particular sequence of actions. Each action leading from a
     * node maps to a unique child node.
     */
    private static final class Node implements SearchNode<Object> {
        private final Node parent;
        private final HashMap<Object, Node> children = new HashMap<>();

        /** Number of times this node has been visited. */
        private int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private int availableCount = 0;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        private Node(int numPlayers, Node parent) {
            this.parent = parent;
            totalScores = new double[numPlayers];
        }

        @Override
        public Node getChild(Object action) {
            return children.get(action);
        }

        @Override
        public void createChild(Object action) {
            if(children.get(action) == null)
                children.put(action, new Node(totalScores.length, this));
        }

        @Override
        public int visitCount() {
            return visitCount;
        }

        @Override
        public int availableCount() {
            return availableCount;
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
                    ", availableCount=" + availableCount +
                    ", totalScores=" + Arrays.toString(totalScores) +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "OLMCTS-RP";
    }
}
