package io.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Closed loop MCTS with tree parallelization.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MCTS
 * @see MCTSRP
 */
public final class MCTSTP implements MCTS {
    @Override
    public
    <STATE extends VisibleState<STATE, ? extends ACTION>, ACTION extends MCTS.Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        final Node<STATE, ACTION> rootNode = new Node<>(numPlayers, rootState, null);
        // -------------------------------

        if(rootNode.validActions.isEmpty())
            return new SearchResults<>(null, 0, 0);

        // Start parallel searches. The main thread does one of them.
        final Thread[] workers = new Thread[params.threadCount() - 1];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> treeParallelSearch(rootNode, params, rand, iters, start), "mctstp" + workerNum);
            workers[workerNum].start();
        }
        treeParallelSearch(rootNode, params, rand, iters, start);

        // Wait for all threads to finish
        try {
            for(Thread worker : workers)
                worker.join();
        } catch(InterruptedException ignored) {
            for(Thread worker : workers)
                worker.interrupt();
        }

        // Recommend the most selected action.
        final double itersPerThread = (double) iters.get() / params.threadCount();
        final ACTION bestAction = io.github.wallacewatler.javamcts.Node.mostVisited(rootNode, rootNode.validActions, rand);
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start);
    }

    private static
    <STATE extends VisibleState<STATE, ? extends ACTION>, ACTION extends MCTS.Action<STATE>>
    void treeParallelSearch(Node<STATE, ACTION> rootNode, SearchParameters params, Random rand, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();
        // TODO: Guarantee that no more than params.maxIters() iterations happen, maybe a semaphore?
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters.get() < params.maxIters())) {
            // iters is incremented immediately to notify other threads that an iteration is in progress.
            iters.getAndIncrement();

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
    }

    /**
     * A node in an MCTS-TP search tree. The tree is structured such that each node stores a game state, and each action
     * leading from a node maps to a unique child node. Locks are used to prevent data corruption from simultaneous
     * reads and writes.
     */
    private static final class Node<STATE extends VisibleState<STATE, ? extends ACTION>, ACTION extends MCTS.Action<STATE>> implements io.github.wallacewatler.javamcts.Node<ACTION> {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private final Node<STATE, ACTION> parent;
        private final ConcurrentHashMap<ACTION, Node<STATE, ACTION>> children;
        private final STATE state;

        /** Cached {@code state.validActions()}. */
        private final List<? extends ACTION> validActions;

        /** Cached {@code state.scores()}. */
        private final double[] scores;

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        private Node(int numPlayers, STATE state, Node<STATE, ACTION> parent) {
            this.parent = parent;
            this.state = state;
            validActions = state.validActions();
            scores = state.scores();
            totalScores = new double[numPlayers];
            children = new ConcurrentHashMap<>(validActions.size());
        }

        @Override
        public Node<STATE, ACTION> getChild(ACTION action) {
            return children.get(action);
        }

        @Override
        public synchronized void createChild(ACTION action) {
            if(!children.containsKey(action)) {
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
            return statsLock;
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
                    ", totalReward=" + Arrays.toString(totalScores) +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MCTS-TP";
    }
}
