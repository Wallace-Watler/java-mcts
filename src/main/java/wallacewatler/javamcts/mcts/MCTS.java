package wallacewatler.javamcts.mcts;

import wallacewatler.javamcts.SearchParameters;
import wallacewatler.javamcts.SearchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monte Carlo tree search (MCTS).
 * <p>
 * This is classic MCTS.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see State
 * @see Action
 * @see MCTS#search
 */
public final class MCTS {
    private MCTS() {}

    /**
     * Search a game tree using classic MCTS.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state to begin search from
     * @param params the search parameters
     *
     * @return The optimal action.
     *
     * @param <STATE> the type of state this search will operate on
     * @param <ACTION> the type of action this search will operate on
     */
    public static
    <STATE extends State<STATE, ACTION>, ACTION extends Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params) {
        // These are shared across threads
        final long start = System.currentTimeMillis();
        final Node<STATE, ACTION> rootNode = new Node<>(numPlayers, rootState, null);
        final Random rand = new Random();
        final AtomicInteger iters = new AtomicInteger();
        // -------------------------------

        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> parallelIters(rootNode, params, rand, iters, start), "mcts" + workerNum);
            workers[workerNum].start();
        }

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
        for(int i = 0; i < rootNode.availableActions.size(); i++) {
            final Node<STATE, ACTION> child = rootNode.getChild(i);
            final int visitCount = child == null ? 0 : child.getVisitCount();
            if(visitCount > maxVisits) {
                maxVisits = visitCount;
                maxActions.clear();
                maxActions.add(rootNode.availableActions.get(i));
            } else if(visitCount == maxVisits) {
                maxActions.add(rootNode.availableActions.get(i));
            }
        }
        final ACTION optimalAction = maxActions.get(rand.nextInt(maxActions.size()));
        return new SearchResults<>(optimalAction, iters.get(), System.currentTimeMillis() - start);
    }

    private static
    <STATE extends State<STATE, ACTION>, ACTION extends Action<STATE>>
    void parallelIters(Node<STATE, ACTION> rootNode, SearchParameters params, Random rand, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters.get() <= params.maxIters())) {
            iters.getAndIncrement();
            Node<STATE, ACTION> currentNode = rootNode;
            currentNode.threadsSearching.getAndIncrement();

            // Selection and Expansion - Select child nodes using the tree policy, expanding where necessary.
            boolean continueSelection = true;
            while(!currentNode.state.isTerminal() && continueSelection) {
                final int selectedAction = currentNode.selectAction(params.uct(), rand);
                Node<STATE, ACTION> selectedChild = currentNode.getOrCreateChild(selectedAction);
                selectedChild.threadsSearching.getAndIncrement();
                if(selectedChild.getVisitCount() == 0)
                    continueSelection = false;

                currentNode = selectedChild;
            }

            // Simulation - Choose a random action until the game ends.
            STATE simulatedState = currentNode.state.copy();
            while(!simulatedState.isTerminal()) {
                final List<ACTION> availableActions = simulatedState.availableActions();
                final ACTION action = availableActions.get(rand.nextInt(availableActions.size()));
                simulatedState = action.applyToState(simulatedState);
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            currentNode.backPropagate(simulatedState.scores());

            now = System.currentTimeMillis();
        }
    }
}
