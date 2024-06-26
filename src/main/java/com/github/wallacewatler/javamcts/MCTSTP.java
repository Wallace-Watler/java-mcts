package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
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
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand, boolean useTTable) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        final Semaphore iterAllowance = new Semaphore(params.maxIters());
        final Node<STATE, ACTION> rootNode = new Node<>(numPlayers, rootState);
        final TranspositionTable<STATE, Node<STATE, ACTION>> transpositionTable = useTTable ? new RealTTable<>() : new DummyTTable<>();
        // -------------------------------

        if(rootNode.validActions().isEmpty())
            return new SearchResults<>(null, 0, 0, 1, 1);

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> treeParallelSearch(rootNode, params, rand, transpositionTable, iterAllowance, iters, start), "mctstp" + workerNum);
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

        // Recommend the most selected action.
        final double itersPerThread = (double) iters.get() / params.threadCount();
        final ACTION bestAction = SearchNode.mostVisited(rootNode, rootNode.validActions(), rand);
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start, rootNode.numNodes(), transpositionTable.size());
    }

    private static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>>
    void treeParallelSearch(Node<STATE, ACTION> rootNode, SearchParameters params, Random rand, TranspositionTable<STATE, Node<STATE, ACTION>> transpositionTable, Semaphore iterAllowance, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();

        final ArrayDeque<Node<STATE, ACTION>> nodePath = new ArrayDeque<>();

        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iterAllowance.tryAcquire())) {
            iters.getAndIncrement();

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
                node.statsLock.writeLock().lock();
                //noinspection NonAtomicOperationOnVolatileField
                node.visitCount++;
                for(int i = 0; i < scores.length; i++)
                    node.totalScores[i] += scores[i];

                node.statsLock.writeLock().unlock();
            }

            now = System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "MCTS-TP";
    }

    /**
     * A node in an MCTS-TP search tree. The tree is structured such that each node stores a game state, and each action
     * leading from a node maps to a unique child node. Locks are used to prevent data corruption from simultaneous
     * reads and writes.
     */
    private static final class Node<STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>> implements SearchNode<ACTION> {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private final ConcurrentHashMap<ACTION, Node<STATE, ACTION>> children;
        private final STATE state;

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        public Node(int numPlayers, STATE state) {
            this.state = state;
            totalScores = new double[numPlayers];
            children = new ConcurrentHashMap<>();
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
            return statsLock;
        }

        public synchronized void createChildIfNotPresent(ACTION action, TranspositionTable<STATE, Node<STATE, ACTION>> transpositionTable) {
            if(!children.containsKey(action)) {
                final STATE state = action.applyToState(this.state.copy());
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (transpositionTable) {
                    if(transpositionTable.contains(state)) {
                        children.put(action, transpositionTable.get(state));
                    } else {
                        final Node<STATE, ACTION> child = new Node<>(totalScores.length, state);
                        children.put(action, child);
                        transpositionTable.put(state, child);
                    }
                }
            }
        }

        public List<ACTION> validActions() {
            return state.validActions();
        }

        public double[] scores() {
            return state.scores();
        }

        /**
         * @return The number of nodes in this tree.
         */
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
