package io.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

/**
 * Multiple-observer information set Monte Carlo tree search (MO-ISMCTS) is applicable to games of imperfect
 * information, where knowledge of the game state can vary between players. Each player maintains an information set
 * that represents this knowledge. Furthermore, players do not observe other players' actions directly since certain
 * aspects of those actions may be hidden. Instead, players observe moves, which are equivalence classes on actions. It
 * is assumed that every player has full view of their own actions. Players cannot observe other players' information
 * sets; all knowledge is gained through observing moves. MO-ISMCTS can be performed by calling the static method
 * {@link MOISMCTS#search}.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see State
 * @see Action
 * @see InfoSet
 * @see Move
 */
public final class MOISMCTS {
    private MOISMCTS() {}

    /**
     * Perform MO-ISMCTS from a given information set. {@code rand} is used for both MO-ISMCTS itself and for game
     * mechanics.
     *
     * @param numPlayers the number of players in the game
     * @param infoSet the information set of the player doing the search
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The search results.
     *
     * @throws IllegalArgumentException if {@code numPlayers} is less than 1 or if {@code infoSet} has no valid actions
     *
     * @param <STATE> the type of state this search will operate on
     * @param <ACTION> the type of action this search will operate on
     *
     * @apiNote {@code rand} can be seeded to reproduce single-threaded runs of MO-ISMCTS. Parallelized runs are not
     * reproducible due to the asynchronous nature of multithreading.
     *
     * @implNote The tree will be discarded once search is complete. Trees are not reused between searches because
     * (TODO: insert long-winded reason here).
     *
     * @see State
     * @see Action
     * @see InfoSet
     * @see Move
     */
    public static
    <STATE extends State<? extends ACTION>, ACTION extends Action<STATE, ?>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<? extends STATE, ? super ACTION> infoSet, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(infoSet.determinize(rand).validActions().isEmpty())
            throw new IllegalArgumentException("infoSet has no valid actions");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        // The root node for each player's tree
        final ArrayList<Node> rootNodes = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            rootNodes.add(new Node(null));
        // -------------------------------

        // Start parallel searches. The main thread does one of them.
        final Thread[] workers = new Thread[params.threadCount() - 1];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> parallelIters(infoSet, rootNodes, params, rand, iters, start), "moismcts" + workerNum);
            workers[workerNum].start();
        }
        parallelIters(infoSet, rootNodes, params, rand, iters, start);

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
        final Node rootNode = rootNodes.get(activePlayer);
        final ArrayList<ACTION> maxActions = new ArrayList<>();
        int maxVisits = 0;
        for(ACTION action : infoSet.determinize(rand).validActions()) { // TODO: InfoSet should have validActions() defined
            final Node child = rootNode.getChild(action.observe(activePlayer));
            final int visitCount = child == null ? 0 : child.visitCount;
            if(visitCount > maxVisits) {
                maxVisits = visitCount;
                maxActions.clear();
                maxActions.add(action);
            } else if(visitCount == maxVisits) {
                maxActions.add(action);
            }
        }
        final ACTION bestAction = maxActions.get(rand.nextInt(maxActions.size()));
        return new SearchResults<>(bestAction, iters.get(), System.currentTimeMillis() - start);
    }

    private static
    <STATE extends State<? extends ACTION>, ACTION extends Action<STATE, ?>>
    void parallelIters(InfoSet<? extends STATE, ? super ACTION> infoSet, ArrayList<Node> rootNodes, SearchParameters params, Random rand, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();

        // Pre-allocated list of nodes. The node at i is the current node in player i's tree.
        final ArrayList<Node> currentNodes = new ArrayList<>(rootNodes.size());

        // TODO: Guarantee that no more than params.maxIters() iterations happen, maybe a semaphore?
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters.get() <= params.maxIters())) {
            // iters is incremented immediately to notify other threads that an iteration is in progress.
            iters.getAndIncrement();

            currentNodes.clear();
            currentNodes.addAll(rootNodes);
            currentNodes.stream().parallel().forEach(node -> node.threadsSearching.getAndIncrement());

            // Current node in the tree of the active player
            Node activeNode = currentNodes.get(infoSet.owner());

            // Choose a random determinized state consistent with the information set of the player searching the tree.
            STATE simulatedState = infoSet.determinize(rand);
            List<? extends ACTION> validActions = simulatedState.validActions();
            infoSet.removePoorActions(validActions); // TODO: This will error

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(simulatedState.scores() == null && continueSelection) {
                final int activePlayer = simulatedState.activePlayer();
                for(ACTION action : validActions)
                    activeNode.createChildIfNotPresent(action.observe(activePlayer));

                final ACTION selectedAction = activeNode.selectAction(params.uct(), rand, activePlayer, validActions);

                // Descend through each player's tree.
                IntStream.range(0, currentNodes.size())
                        .parallel()
                        .forEach(i -> {
                            final Node node = currentNodes.get(i);
                            final Node child = node.getOrCreateChild(selectedAction.observe(i));
                            child.threadsSearching.getAndIncrement();
                            currentNodes.set(i, child);
                        });

                final Node selectedChild = activeNode.getChild(selectedAction.observe(activePlayer));
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                for(ACTION action : validActions)
                    activeNode.getChild(action.observe(activePlayer)).incAvailableCount();

                simulatedState = selectedAction.applyToState(simulatedState, rand);
                validActions = simulatedState.validActions();

                activeNode = currentNodes.get(simulatedState.activePlayer());
            }

            // Simulation - Choose a random action until the game is decided.
            while(simulatedState.scores() == null) {
                final ACTION action = validActions.get(rand.nextInt(validActions.size()));
                simulatedState = action.applyToState(simulatedState, rand);
                validActions = simulatedState.validActions();
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            final double[] scores = simulatedState.scores();
            IntStream.range(0, currentNodes.size())
                    .parallel()
                    .forEach(i -> currentNodes.get(i).backPropagate(scores[i]));

            now = System.currentTimeMillis();
        }
    }

    /**
     * A node in an MO-ISMCTS search tree. A node represents a sequence of moves from the beginning of the game. Each
     * move leading from a node maps to a unique child node.
     */
    private static final class Node {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private final Node parent;
        private final HashMap<Object, Node> children = new HashMap<>();

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private volatile int availableCount = 0;

        /** Total score from going through this node. */
        private volatile double totalScore = 0.0;

        /** Number of threads currently searching through this node. */
        private final AtomicInteger threadsSearching = new AtomicInteger();

        private Node(Node parent) {
            this.parent = parent;
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

            threadsSearching.getAndDecrement();
            if(parent != null)
                parent.backPropagate(score);
        }

        private Node getChild(Object move) {
            return children.get(move);
        }

        private synchronized void createChildIfNotPresent(Object move) {
            if(!children.containsKey(move))
                children.put(move, new Node(this));
        }

        private Node getOrCreateChild(Object move) {
            createChildIfNotPresent(move);
            return getChild(move);
        }

        private <ACTION extends Action<?, ?>> ACTION selectAction(UCT uct, Random rand, int activePlayer, List<ACTION> validActions) {
            if(visitCount == 0)
                return validActions.get(rand.nextInt(validActions.size()));

            final ArrayList<ACTION> maxActions = new ArrayList<>();
            double maxUctValue = Double.NEGATIVE_INFINITY;

            statsLock.readLock().lock();
            for(ACTION action : validActions) {
                final Node child = getChild(action.observe(activePlayer));
                final double uctValue;
                if(child.threadsSearching.get() > 0) {
                    uctValue = Double.NEGATIVE_INFINITY;
                } else if(child.availableCount == 0 || child.visitCount == 0) {
                    uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalScore / visitCount);
                } else {
                    child.statsLock.readLock().lock();
                    final double exploitation = child.totalScore / child.visitCount;
                    final double exploration = uct.explorationParam() * Math.sqrt(Math.log(child.availableCount) / child.visitCount);
                    child.statsLock.readLock().unlock();
                    uctValue = exploitation + exploration;
                }

                if(uctValue == maxUctValue) {
                    maxActions.add(action);
                } else if(uctValue > maxUctValue) {
                    maxUctValue = uctValue;
                    maxActions.clear();
                    maxActions.add(action);
                }
            }
            statsLock.readLock().unlock();

            return maxActions.get(rand.nextInt(maxActions.size()));
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
}
