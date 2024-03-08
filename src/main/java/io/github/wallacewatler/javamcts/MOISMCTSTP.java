package io.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    <STATE extends State<? extends ACTION>, ACTION extends Action<STATE, ? extends MOVE>, MOVE extends Move<? extends ACTION>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<? extends STATE, ? super MOVE> infoSet, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(infoSet.determinize(rand).validActions().isEmpty()) // TODO: InfoSet should have validActions() defined
            return new SearchResults<>(null, 0, 0);

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        // The root node for each player's tree
        final ArrayList<Node<MOVE>> rootNodes = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            rootNodes.add(new Node<>(null));
        // -------------------------------

        // Start parallel searches. The main thread does one of them.
        final Thread[] workers = new Thread[params.threadCount() - 1];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> treeParallelSearch(infoSet, rootNodes, params, rand, iters, start), "moismctstp" + workerNum);
            workers[workerNum].start();
        }
        treeParallelSearch(infoSet, rootNodes, params, rand, iters, start);

        // Wait for all threads to finish
        try {
            for(Thread worker : workers)
                worker.join();
        } catch(InterruptedException ignored) {
            for(Thread worker : workers)
                worker.interrupt();
        }

        // Recommend the most selected action. Ties are broken by randomness.
        final int activePlayer = infoSet.owner();
        // TODO: InfoSet should have validActions() defined
        final List<? extends MOVE> validMoves = infoSet.determinize(rand).validActions().stream().map(action -> action.observe(activePlayer)).toList();
        final MOVE bestMove = io.github.wallacewatler.javamcts.Node.mostVisited(rootNodes.get(activePlayer), validMoves, rand);
        final double itersPerThread = (double) iters.get() / params.threadCount();
        return new SearchResults<>(bestMove.asAction(), itersPerThread, System.currentTimeMillis() - start);
    }

    private static
    <STATE extends State<? extends ACTION>, ACTION extends Action<STATE, ? extends MOVE>, MOVE extends Move<? extends ACTION>>
    void treeParallelSearch(InfoSet<? extends STATE, ? super MOVE> infoSet, ArrayList<Node<MOVE>> rootNodes, SearchParameters params, Random rand, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();

        // Pre-allocated list of nodes. The node at i is the current node in player i's tree.
        final ArrayList<Node<MOVE>> currentNodes = new ArrayList<>(rootNodes.size());

        final Function<STATE, List<? extends MOVE>> stateValidMoves = state -> state.validActions().stream()
                .map(action -> action.observe(state.activePlayer()))
                .toList();

        // TODO: Guarantee that no more than params.maxIters() iterations happen, maybe a semaphore?
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters.get() < params.maxIters())) {
            // iters is incremented immediately to notify other threads that an iteration is in progress.
            iters.getAndIncrement();

            currentNodes.clear();
            currentNodes.addAll(rootNodes);

            // Current node in the tree of the active player
            Node<MOVE> activeNode = currentNodes.get(infoSet.owner());

            // Choose a random determinized state consistent with the information set of the player searching the tree.
            STATE simulatedState = infoSet.determinize(rand);
            List<? extends MOVE> validMoves = stateValidMoves.apply(simulatedState);
            infoSet.removePoorMoves(validMoves); // TODO: This will error

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(simulatedState.scores() == null && continueSelection) {
                final int activePlayer = simulatedState.activePlayer();

                for(MOVE move : validMoves)
                    activeNode.createChild(move);

                final MOVE selectedMove = params.uct().selectBranch(activeNode, rand, activePlayer, validMoves);
                final Node<MOVE> selectedChild = activeNode.getChild(selectedMove);
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                for(MOVE move : validMoves)
                    activeNode.getChild(move).incAvailableCount();

                final ACTION selectedAction = selectedMove.asAction();

                // Descend through each player's tree.
                for(int pov = 0; pov < currentNodes.size(); pov++) {
                    final MOVE move = selectedAction.observe(pov);
                    final Node<MOVE> node = currentNodes.get(pov);
                    node.createChild(move);
                    final Node<MOVE> child = node.getChild(move);
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
    private static final class Node<MOVE> implements io.github.wallacewatler.javamcts.Node<MOVE> {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private final Node<MOVE> parent;
        private final ConcurrentHashMap<MOVE, Node<MOVE>> children = new ConcurrentHashMap<>();

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private volatile int availableCount = 0;

        /** Total score from going through this node. */
        private volatile double totalScore = 0.0;

        private Node(Node<MOVE> parent) {
            this.parent = parent;
        }

        @Override
        public Node<MOVE> getChild(MOVE move) {
            return children.get(move);
        }

        @Override
        public synchronized void createChild(MOVE move) {
            if(!children.containsKey(move))
                children.put(move, new Node<>(this));
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
            return totalScore;
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

        private void backPropagate(double score) {
            statsLock.writeLock().lock();
            visitCount++;
            totalScore += score;
            statsLock.writeLock().unlock();

            if(parent != null)
                parent.backPropagate(score);
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
