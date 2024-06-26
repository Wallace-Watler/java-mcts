package com.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * MO-ISMCTS with tree parallelization.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MOISMCTS
 * @see MOISMCTSRP
 */
public final class MOISMCTSTP implements MOISMCTS {
    @Override
    public
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, MOVE extends Move<ACTION>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<STATE, MOVE> infoSet, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(infoSet.validMoves().isEmpty())
            return new SearchResults<>(null, 0, 0, 1, 1);

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        final Semaphore iterAllowance = new Semaphore(params.maxIters());
        // The root node for each player's tree
        final ArrayList<Node> rootNodes = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            rootNodes.add(new Node(null));
        // -------------------------------

        // Start parallel searches.
        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> treeParallelSearch(infoSet, rootNodes, params, rand, iterAllowance, iters, start), "moismctstp" + workerNum);
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

        // Recommend the most selected action. Ties are broken by randomness.
        final Node rootNode = rootNodes.get(infoSet.owner());
        final MOVE bestMove = SearchNode.mostVisited(rootNode, infoSet.validMoves(), rand);
        final double itersPerThread = (double) iters.get() / params.threadCount();
        return new SearchResults<>(bestMove.asAction(), itersPerThread, System.currentTimeMillis() - start, rootNode.numNodes(), 0);
    }

    private static
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, MOVE extends Move<ACTION>>
    void treeParallelSearch(InfoSet<STATE, MOVE> infoSet, ArrayList<Node> rootNodes, SearchParameters params, Random rand, Semaphore iterAllowance, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();

        // Pre-allocated list of nodes. The node at i is the current node in player i's tree.
        final ArrayList<Node> currentNodes = new ArrayList<>(rootNodes.size());

        final Function<STATE, List<MOVE>> stateValidMoves = state -> state.validActions().stream()
                .map(action -> action.observe(state.activePlayer()))
                .toList();

        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iterAllowance.tryAcquire())) {
            iters.getAndIncrement();

            currentNodes.clear();
            currentNodes.addAll(rootNodes);

            // Current node in the tree of the active player
            Node activeNode = currentNodes.get(infoSet.owner());

            // Choose a random determinized state consistent with the information set of the player searching the tree.
            STATE simulatedState = infoSet.determinize(rand);
            List<MOVE> validMoves = stateValidMoves.apply(simulatedState);

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(simulatedState.scores() == null && continueSelection) {
                for(MOVE move : validMoves)
                    activeNode.createChildIfNotPresent(move);

                final MOVE selectedMove = SearchNode.selectBranch(activeNode, validMoves, simulatedState.activePlayer(), params.uct(), rand);
                final Node selectedChild = activeNode.getChild(selectedMove);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                for(MOVE move : validMoves)
                    activeNode.getChild(move).incAvailableCount();

                final ACTION selectedAction = selectedMove.asAction();

                // Descend through each player's tree.
                for(int pov = 0; pov < currentNodes.size(); pov++) {
                    final MOVE move = selectedAction.observe(pov);
                    final Node node = currentNodes.get(pov);
                    node.createChildIfNotPresent(move);
                    final Node child = node.getChild(move);
                    currentNodes.set(pov, child);
                }

                simulatedState = selectedAction.applyToState(simulatedState, rand);
                validMoves = stateValidMoves.apply(simulatedState);

                activeNode = currentNodes.get(simulatedState.activePlayer());
            }

            // Simulation - Choose a random action until the game is decided.
            while(simulatedState.scores() == null) {
                final ACTION action = validMoves.get(rand.nextInt(validMoves.size())).asAction();
                simulatedState = action.applyToState(simulatedState, rand);
                validMoves = stateValidMoves.apply(simulatedState);
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            final double[] scores = simulatedState.scores();
            IntStream.range(0, currentNodes.size())
                    .parallel()
                    .forEach(pov -> currentNodes.get(pov).backPropagate(scores[pov]));

            now = System.currentTimeMillis();
        }
    }

    /**
     * A node in an MO-ISMCTS-TP search tree. A node represents a sequence of moves from the beginning of the game. Each
     * move leading from a node maps to a unique child node.
     */
    private static final class Node implements SearchNode<Object> {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private final Node parent;
        private final ConcurrentHashMap<Object, Node> children = new ConcurrentHashMap<>();

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private volatile int availableCount = 0;

        /** Total score from going through this node. */
        private volatile double totalScore = 0.0;

        private Node(Node parent) {
            this.parent = parent;
        }

        @Override
        public int visitCount() {
            return visitCount;
        }

        @Override
        public double totalScore(int activePlayer) {
            return totalScore;
        }

        @Override
        public Node getChild(Object move) {
            return children.get(move);
        }

        @Override
        public int selectCount(Object move) {
            return getChild(move).visitCount;
        }

        @Override
        public int availableCount(Object move) {
            return getChild(move).availableCount;
        }

        @Override
        public ReadWriteLock statsLock() {
            return statsLock;
        }

        public synchronized void createChildIfNotPresent(Object move) {
            if(!children.containsKey(move))
                children.put(move, new Node(this));
        }

        @SuppressWarnings("NonAtomicOperationOnVolatileField")
        private void incAvailableCount() {
            statsLock.writeLock().lock();
            availableCount++;
            statsLock.writeLock().unlock();
        }

        @SuppressWarnings("NonAtomicOperationOnVolatileField")
        private void backPropagate(double score) {
            statsLock.writeLock().lock();
            visitCount++;
            totalScore += score;
            statsLock.writeLock().unlock();

            if(parent != null)
                parent.backPropagate(score);
        }

        public int numNodes() {
            int num = 1;
            for(Node child : children.values())
                num += child.numNodes();

            return num;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "visitCount=" + visitCount +
                    ", availableCount=" + availableCount +
                    ", totalScore=" + totalScore +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "MO-ISMCTS-TP";
    }
}
