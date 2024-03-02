package io.github.wallacewatler.javamcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Open-loop Monte Carlo tree search (OLMCTS) is effective on stochastic games of perfect information, games involving
 * non-discrete states, and games of hidden information where said information is hidden from all players. An action may
 * involve randomness such that it can lead to one of many possible states. The revealing of hidden information can be
 * treated as a random event. OLMCTS can be performed by calling the static method {@link OLMCTS#search}.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see VisibleState
 * @see StochasticAction
 */
public final class OLMCTS {
    private OLMCTS() {}

    /**
     * Perform OLMCTS from a given game state. {@code rand} is used for both OLMCTS itself and for game mechanics.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state from which to begin the search
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The search results.
     *
     * @throws IllegalArgumentException if {@code numPlayers} is less than 1 or if {@code rootState} has no valid
     * actions
     *
     * @param <STATE> the type of state the search operates on
     * @param <ACTION> the type of action the search operates on
     *
     * @apiNote {@code rand} can be seeded to reproduce single-threaded runs of OLMCTS. Parallelized runs are not
     * reproducible due to the asynchronous nature of multithreading.
     *
     * @implNote The tree will be discarded once search is complete. Trees are not reused between searches because
     * information is gained after an action is applied to the true game state; previous searches operated without that
     * knowledge (assuming the true state was updated since the last search) and therefore may not be reflective of the
     * true game state's value.
     *
     * @see SearchResults
     */
    public static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends StochasticAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        if(rootState.validActions().isEmpty())
            throw new IllegalArgumentException("rootState has no valid actions");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final Node<ACTION> rootNode = new Node<>(numPlayers, null);
        final AtomicInteger iters = new AtomicInteger();
        // -------------------------------

        // Start parallel searches. The main thread does one of them.
        final Thread[] workers = new Thread[params.threadCount() - 1];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> parallelIters(rootState, rootNode, params, rand, iters, start), "olmcts" + workerNum);
            workers[workerNum].start();
        }
        parallelIters(rootState, rootNode, params, rand, iters, start);

        // Wait for all threads to finish
        try {
            for(Thread worker : workers)
                worker.join();
        } catch(InterruptedException ignored) {
            for(Thread worker : workers)
                worker.interrupt();
        }

        // Recommend the most selected action. Ties are broken at random.
        final ArrayList<ACTION> maxActions = new ArrayList<>();
        int maxVisits = 0;
        for(ACTION action : rootState.validActions()) {
            final Node<ACTION> child = rootNode.getChild(action);
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
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends StochasticAction<STATE>>
    void parallelIters(STATE rootState, Node<ACTION> rootNode, SearchParameters params, Random rand, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();
        // TODO: Guarantee that no more than params.maxIters() iterations happen, maybe a semaphore?
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters.get() <= params.maxIters())) {
            // iters is incremented immediately to notify other threads that an iteration is in progress.
            iters.getAndIncrement();

            Node<ACTION> currentNode = rootNode;
            currentNode.threadsSearching.getAndIncrement();

            STATE simulatedState = rootState.copy();
            List<ACTION> validActions = simulatedState.validActions();

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(simulatedState.scores() == null && continueSelection) {
                final int activePlayer = simulatedState.activePlayer();
                for(ACTION action : validActions)
                    currentNode.createChildIfNotPresent(action);

                final ACTION selectedAction = currentNode.selectAction(params.uct(), rand, activePlayer, validActions);
                final Node<ACTION> selectedChild = currentNode.getChild(selectedAction);
                selectedChild.threadsSearching.getAndIncrement();
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
     * A node in an OLMCTS search tree. Since actions are stochastic and states may be continuous in OLMCTS, a single
     * node represents a distribution of states reached via a particular sequence of actions. Each action leading from a
     * node maps to a unique child node.
     *
     * @param <ACTION> the type of action that links nodes together
     */
    private static final class Node<ACTION> {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private final Node<ACTION> parent;
        private final HashMap<ACTION, Node<ACTION>> children = new HashMap<>();

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Number of times this node has been available for selection. */
        private volatile int availableCount = 0;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        /** Number of threads currently searching through this node. */
        private final AtomicInteger threadsSearching = new AtomicInteger();

        private Node(int numPlayers, Node<ACTION> parent) {
            this.parent = parent;
            totalScores = new double[numPlayers];
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

            threadsSearching.getAndDecrement();
            if(parent != null)
                parent.backPropagate(scores);
        }

        private Node<ACTION> getChild(ACTION action) {
            return children.get(action);
        }

        private synchronized void createChildIfNotPresent(ACTION action) {
            if(!children.containsKey(action))
                children.put(action, new Node<>(totalScores.length, this));
        }

        private ACTION selectAction(UCT uct, Random rand, int activePlayer, List<ACTION> validActions) {
            if(visitCount == 0)
                return validActions.get(rand.nextInt(validActions.size()));

            final ArrayList<ACTION> maxActions = new ArrayList<>();
            double maxUctValue = Double.NEGATIVE_INFINITY;

            statsLock.readLock().lock();
            for(ACTION action : validActions) {
                final Node<ACTION> child = getChild(action);
                final double uctValue;
                if(child.threadsSearching.get() > 0) {
                    uctValue = Double.NEGATIVE_INFINITY;
                } else if(child.availableCount == 0 || child.visitCount == 0) {
                    uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalScores[activePlayer] / visitCount);
                } else {
                    child.statsLock.readLock().lock();
                    final double exploitation = child.totalScores[activePlayer] / child.visitCount;
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
                    ", totalScores=" + Arrays.toString(totalScores) +
                    '}';
        }
    }
}
