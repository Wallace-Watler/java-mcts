package io.github.wallacewatler.javamcts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <strong>Closed loop Monte Carlo tree search</strong><br>
 * This is classic MCTS, effective on deterministic games of perfect information. The entire game state and all players'
 * actions are visible to everyone, and every action has a pre-determined effect on the state.<br>
 * <br>
 * An {@code MCTS} object acts as a search tree. During a search, the tree will be built, and it will remain intact so
 * that future searches can be guided by previous ones.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this MCTS operates on
 * @param <ACTION> the type of action this MCTS operates on
 *
 * @see VisibleState
 * @see DeterministicAction
 */
public final class MCTS<STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>> {
    /**
     * Perform MCTS from a given game state. This method is equivalent to
     * {@code new MCTS<>(numPlayers, rootState).search(params, rand)}.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state from which to begin the search
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The search results.
     *
     * @param <STATE> the type of state the search operates on
     * @param <ACTION> the type of action the search operates on
     *
     * @see MCTS#search(SearchParameters, Random)
     */
    public static
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand) {
        return new MCTS<>(numPlayers, rootState).search(params, rand);
    }

    /** The number of players in the game. */
    public final int numPlayers;
    private Node rootNode;

    /**
     * Create a new MCTS search tree.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state from which to begin searches; in other words, the true state of the game
     *
     * @throws IllegalArgumentException if {@code numPlayers} is less than 1
     */
    public MCTS(int numPlayers, STATE rootState) {
        if(numPlayers < 1)
            throw new IllegalArgumentException("numPlayers must be at least 1");

        this.numPlayers = numPlayers;
        rootNode = new Node(rootState, null);
    }

    /**
     * @return The current root state.
     */
    public STATE getRootState() {
        return rootNode.state;
    }

    /**
     * @return True if the root state has no valid actions.
     */
    public boolean hasGameEnded() {
        return rootNode.validActions.isEmpty();
    }

    /**
     * TODO
     *
     * @param action an action
     *
     * @throws IllegalArgumentException if {@code action} is not valid in the root state
     */
    public void advanceGame(ACTION action) {
        for(int i = 0; i < rootNode.validActions.size(); i++) {
            final ACTION a = rootNode.validActions.get(i);
            if(a.equals(action)) {
                rootNode = rootNode.getOrCreateChild(i);
                rootNode.parent = null;
                return;
            }
        }
        throw new IllegalArgumentException("action " + action + " not available");
    }

    /**
     * Perform MCTS on this tree. The root state must have valid actions to perform the search; this can be checked with
     * {@link MCTS#hasGameEnded()}. {@code rand} is only used for MCTS itself, not for any game mechanics.
     *
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The search results.
     *
     * @throws IllegalStateException if the root state of this tree has no valid actions
     *
     * @apiNote {@code rand} can be seeded to reproduce single-threaded runs of MCTS. Parallelized runs are not
     * reproducible due to the asynchronous nature of multithreading.
     */
    public SearchResults<ACTION> search(SearchParameters params, Random rand) {
        if(hasGameEnded())
            throw new IllegalStateException("the root state has no valid actions");

        // These are shared across threads
        final long start = System.currentTimeMillis();
        final AtomicInteger iters = new AtomicInteger();
        // -------------------------------

        // Start parallel searches. The main thread does one of them.
        final Thread[] workers = new Thread[params.threadCount() - 1];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> parallelIters(params, rand, iters, start), "mcts" + workerNum);
            workers[workerNum].start();
        }
        parallelIters(params, rand, iters, start);

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
        for(int i = 0; i < rootNode.validActions.size(); i++) {
            final Node child = rootNode.getChild(i);
            final int visitCount = child == null ? 0 : child.visitCount;
            if(visitCount > maxVisits) {
                maxVisits = visitCount;
                maxActions.clear();
                maxActions.add(rootNode.validActions.get(i));
            } else if(visitCount == maxVisits) {
                maxActions.add(rootNode.validActions.get(i));
            }
        }
        final ACTION bestAction = maxActions.get(rand.nextInt(maxActions.size()));
        return new SearchResults<>(bestAction, iters.get(), System.currentTimeMillis() - start);
    }

    private void parallelIters(SearchParameters params, Random rand, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();
        // TODO: Guarantee that no more than params.maxIters() iterations happen, maybe a semaphore?
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters.get() <= params.maxIters())) {
            // iters is incremented immediately to notify other threads that an iteration is in progress.
            iters.getAndIncrement();

            Node currentNode = rootNode;
            currentNode.threadsSearching.getAndIncrement();

            // Selection and Expansion - Select child nodes using UCT, expanding where necessary.
            boolean continueSelection = true;
            while(currentNode.scores == null && continueSelection) {
                final int selectedAction = currentNode.selectAction(params.uct(), rand);
                final Node selectedChild = currentNode.getOrCreateChild(selectedAction);
                selectedChild.threadsSearching.getAndIncrement();
                if(selectedChild.visitCount == 0)
                    continueSelection = false;

                currentNode = selectedChild;
            }

            // Simulation - Choose a random action until the game is decided.
            STATE simulatedState = currentNode.state.copy();
            while(simulatedState.scores() == null) {
                final List<ACTION> validActions = simulatedState.validActions();
                final ACTION action = validActions.get(rand.nextInt(validActions.size()));
                simulatedState = action.applyToState(simulatedState);
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            currentNode.backPropagate(simulatedState.scores());

            now = System.currentTimeMillis();
        }
    }

    /**
     * A node in an MCTS search tree. The tree is structured such that each node stores a game state, and each action
     * leading from a node maps to a unique child node.
     */
    private final class Node {
        private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();
        private Node parent;
        private final ArrayList<Node> children; // TODO: Try a HashMap<ACTION, Node> if it's not too slow
        private final STATE state;

        /** Cached {@code state.validActions()}. */
        private final List<ACTION> validActions;

        /** Cached {@code state.scores()}. */
        private final double[] scores;

        /** Number of times this node has been visited. */
        private volatile int visitCount = 0;

        /** Total score that each player obtains by going through this node. */
        private final double[] totalScores;

        /** Number of threads currently searching through this node. */
        private final AtomicInteger threadsSearching = new AtomicInteger();

        private Node(STATE state, Node parent) {
            this.parent = parent;
            this.state = state;
            validActions = state.validActions();
            scores = state.scores();
            totalScores = new double[numPlayers];

            children = new ArrayList<>(validActions.size());
            for(ACTION ignored : validActions)
                children.add(null);
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

        private Node getChild(int action) {
            return children.get(action);
        }

        private synchronized Node getOrCreateChild(int action) {
            if(children.get(action) == null) {
                final STATE state = validActions.get(action).applyToState(this.state.copy());
                children.set(action, new Node(state, this));
            }
            return children.get(action);
        }

        private int selectAction(UCT uct, Random rand) {
            if(visitCount == 0)
                return rand.nextInt(validActions.size());

            final int player = state.activePlayer();
            final ArrayList<Integer> maxActions = new ArrayList<>();
            double maxUctValue = Double.NEGATIVE_INFINITY;

            statsLock.readLock().lock();
            for(int i = 0; i < validActions.size(); i++) {
                final Node child = children.get(i);
                final double uctValue;
                if(child == null) {
                    uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalScores[player] / visitCount);
                } else if(child.threadsSearching.get() > 0) {
                    uctValue = Double.NEGATIVE_INFINITY;
                } else if(child.visitCount == 0) {
                    uctValue = uct.favorUnexplored() ? Double.POSITIVE_INFINITY : (totalScores[player] / visitCount);
                } else {
                    child.statsLock.readLock().lock();
                    final double exploitation = child.totalScores[player] / child.visitCount;
                    final double exploration = uct.explorationParam() * Math.sqrt(Math.log(visitCount) / child.visitCount);
                    child.statsLock.readLock().unlock();
                    uctValue = exploitation + exploration;
                }

                if(uctValue == maxUctValue) {
                    maxActions.add(i);
                } else if(uctValue > maxUctValue) {
                    maxUctValue = uctValue;
                    maxActions.clear();
                    maxActions.add(i);
                }
            }
            statsLock.readLock().unlock();

            return maxActions.get(rand.nextInt(maxActions.size()));
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
