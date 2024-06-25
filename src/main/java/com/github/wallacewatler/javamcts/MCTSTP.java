package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
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
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        final Semaphore iterAllowance = new Semaphore(params.maxIters());
        final Node<STATE, ACTION> rootNode = new Node<>(numPlayers, rootState);
        // -------------------------------

        if(rootNode.validActions().isEmpty())
            return new SearchResults<>(null, 0, 0);

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> treeParallelSearch(rootNode, params, rand, iterAllowance, iters, start), "mctstp" + workerNum);
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
        return new SearchResults<>(bestAction, itersPerThread, System.currentTimeMillis() - start);
    }

    private static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>>
    void treeParallelSearch(Node<STATE, ACTION> rootNode, SearchParameters params, Random rand, Semaphore iterAllowance, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();

        final ArrayDeque<Node<STATE, ACTION>> path = new ArrayDeque<>();

        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iterAllowance.tryAcquire())) {
            iters.getAndIncrement();

            Node<STATE, ACTION> currentNode = rootNode;
            path.add(currentNode);

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(currentNode.scores() == null && continueSelection) {
                final ACTION selectedAction = selectAction(currentNode, rand, params.uct());
                currentNode.createChildIfNotPresent(selectedAction);
                final Node<STATE, ACTION> selectedChild = currentNode.getChild(selectedAction);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                currentNode = selectedChild;
                path.add(currentNode);
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
            while(!path.isEmpty()) {
                final Node<STATE, ACTION> node = path.removeLast();
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

    private static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends MCTS.Action<STATE>>
    ACTION selectAction(Node<STATE, ACTION> parent, Random rand, UCT uct) {
        final List<ACTION> actions = parent.validActions();
        final int activePlayer = parent.state.activePlayer();
        if(parent.visitCount == 0)
            return actions.get(rand.nextInt(actions.size()));

        final ArrayList<ACTION> maxBranches = new ArrayList<>();
        double maxUctValue = Double.NEGATIVE_INFINITY;

        parent.statsLock.readLock().lock();
        for(ACTION action : actions) {
            final Node<STATE, ACTION> child = parent.getChild(action);
            final double uctValue;
            if(child == null || child.visitCount == 0) {
                uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (parent.totalScores[activePlayer] / parent.visitCount);
            } else {
                child.statsLock.readLock().lock();
                final double exploitation = child.totalScores[activePlayer] / child.visitCount;
                final double exploration = uct.explorationParam() * Math.sqrt(Math.log(parent.visitCount) / child.visitCount);
                child.statsLock.readLock().unlock();
                uctValue = exploitation + exploration;
            }

            if(uctValue == maxUctValue) {
                maxBranches.add(action);
            } else if(uctValue > maxUctValue) {
                maxUctValue = uctValue;
                maxBranches.clear();
                maxBranches.add(action);
            }
        }
        parent.statsLock.readLock().unlock();

        return maxBranches.get(rand.nextInt(maxBranches.size()));
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
        public Node<STATE, ACTION> getChild(ACTION action) {
            return children.get(action);
        }

        public synchronized void createChildIfNotPresent(ACTION action) {
            if(!children.containsKey(action)) {
                final STATE state = action.applyToState(this.state.copy());
                children.put(action, new Node<>(totalScores.length, state));
            }
        }

        public List<ACTION> validActions() {
            return state.validActions();
        }

        public double[] scores() {
            return state.scores();
        }

        @Override
        public int visitCount() {
            return visitCount;
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
