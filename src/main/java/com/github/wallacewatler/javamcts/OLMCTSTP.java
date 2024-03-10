package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Open-loop MCTS with tree-parallelization.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see OLMCTSRP
 */
public final class OLMCTSTP implements OLMCTS {
    @Override
    public
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(rootState.validActions().isEmpty())
            throw new IllegalArgumentException("rootState has no valid actions");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final Node rootNode = new Node(numPlayers, null);
        final AtomicInteger iters = new AtomicInteger();
        final Semaphore iterAllowance = new Semaphore(params.maxIters());
        // -------------------------------

        // Start parallel searches. The main thread does one of them.
        final Thread[] workers = new Thread[params.threadCount() - 1];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> treeParallelSearch(rootState, rootNode, params, rand, iterAllowance, iters, start), "olmctstp" + workerNum);
            workers[workerNum].start();
        }
        treeParallelSearch(rootState, rootNode, params, rand, iterAllowance, iters, start);

        // Wait for all threads to finish
        try {
            for(Thread worker : workers)
                worker.join();
        } catch(InterruptedException ignored) {
            for(Thread worker : workers)
                worker.interrupt();
        }

        final ACTION bestAction = SearchNode.mostVisited(rootNode, rootState.validActions(), rand);
        final double itersPerThread = (double) iters.get() / params.threadCount();
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start);
    }

    private static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends Action<STATE>>
    void treeParallelSearch(STATE rootState, Node rootNode, SearchParameters params, Random rand, Semaphore iterAllowance, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();

        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iterAllowance.tryAcquire())) {
            iters.getAndIncrement();

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
                    currentNode.getChild(action).incAvailableCount();

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
    }

    /**
     * A node in an OLMCTS-TP search tree. Since actions are stochastic and states may be continuous in OLMCTS, a single
     * node represents a distribution of states reached via a particular sequence of actions. Each action leading from a
     * node maps to a unique child node.
     */
    private static final class Node implements SearchNode<Object> {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private final Node parent;
        private final ConcurrentHashMap<Object, Node> children = new ConcurrentHashMap<>();

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private volatile int availableCount = 0;

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
        public synchronized void createChild(Object action) {
            if(!children.containsKey(action))
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
            return statsLock;
        }

        private void incAvailableCount() {
            statsLock.writeLock().lock();
            availableCount++;
            statsLock.writeLock().unlock();
        }

        private void backPropagate(double[] scores) {
            statsLock.writeLock().lock();
            visitCount++;
            for(int i = 0; i < scores.length; i++)
                totalScores[i] += scores[i];

            statsLock.writeLock().unlock();

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
        return "OLMCTS-TP";
    }
}
