package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
            rootNodes.add(new Node(numPlayers));

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            final Node rootNode = rootNodes.get(workerNum);
            workers[workerNum] = new Thread(() -> totalIters.addAndGet(rootParallelSearch(rootState, rootNode, params, rand, start)), "olmctsrp" + workerNum);
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

        final ArrayDeque<Node> path = new ArrayDeque<>();

        int iters = 0;
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters < params.maxIters())) {
            iters++;

            Node currentNode = rootNode;
            path.add(currentNode);
            STATE simulatedState = rootState.copy();
            List<ACTION> validActions = simulatedState.validActions();

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(simulatedState.scores() == null && continueSelection) {
                final int activePlayer = simulatedState.activePlayer();
                for(ACTION action : validActions)
                    currentNode.createChildIfNotPresent(action);

                final ACTION selectedAction = selectAction(currentNode, rand, activePlayer, validActions, params.uct());
                final Node selectedChild = currentNode.getChild(selectedAction);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                for(ACTION action : validActions)
                    currentNode.getChild(action).availableCount++;

                simulatedState = selectedAction.applyToState(simulatedState, rand);
                validActions = simulatedState.validActions();

                currentNode = selectedChild;
                path.add(currentNode);
            }

            // Simulation - Choose a random action until the game is decided.
            while(simulatedState.scores() == null) {
                final ACTION action = validActions.get(rand.nextInt(validActions.size()));
                simulatedState = action.applyToState(simulatedState, rand);
                validActions = simulatedState.validActions();
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            final double[] scores = simulatedState.scores();
            while(!path.isEmpty()) {
                final Node node = path.removeLast();
                node.visitCount++;
                for(int i = 0; i < scores.length; i++)
                    node.totalScores[i] += scores[i];
            }

            now = System.currentTimeMillis();
        }
        return iters;
    }

    private static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends OLMCTS.Action<STATE>>
    ACTION selectAction(Node parent, Random rand, int activePlayer, List<ACTION> actions, UCT uct) {
        if(parent.visitCount == 0)
            return actions.get(rand.nextInt(actions.size()));

        final ArrayList<ACTION> maxBranches = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        for(ACTION branch : actions) {
            final Node child = parent.getChild(branch);
            final double uctValue;
            if(child == null || child.availableCount == 0 || child.visitCount == 0) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (parent.totalScores[activePlayer] / parent.visitCount);
            } else {
                final double exploitation = child.totalScores[activePlayer] / child.visitCount;
                final double exploration = uct.explorationParam() * Math.sqrt(Math.log(child.availableCount) / child.visitCount);
                uctValue = exploitation + exploration;
            }

            if(uctValue == maxUctValue) {
                maxBranches.add(branch);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxBranches.clear();
                maxBranches.add(branch);
            }
        }

        return maxBranches.get(rand.nextInt(maxBranches.size()));
    }

    /**
     * A node in an OLMCTS-RP search tree. Since actions are stochastic and states may be continuous in OLMCTS, a single
     * node represents a distribution of states reached via a particular sequence of actions. Each action leading from a
     * node maps to a unique child node.
     */
    private static final class Node implements SearchNode<Object> {
        private final HashMap<Object, Node> children = new HashMap<>();

        /** Number of times this node has been visited. */
        private int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private int availableCount = 0;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        private Node(int numPlayers) {
            totalScores = new double[numPlayers];
        }

        @Override
        public Node getChild(Object action) {
            return children.get(action);
        }

        public void createChildIfNotPresent(Object action) {
            if(children.get(action) == null)
                children.put(action, new Node(totalScores.length));
        }

        @Override
        public int visitCount() {
            return visitCount;
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
