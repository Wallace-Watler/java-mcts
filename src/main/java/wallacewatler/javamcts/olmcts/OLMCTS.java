package wallacewatler.javamcts.olmcts;

import wallacewatler.javamcts.SearchParameters;
import wallacewatler.javamcts.SearchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Open-loop Monte Carlo tree search (OLMCTS).
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see State
 * @see Action
 * @see OLMCTS#search
 */
public final class OLMCTS {
    private OLMCTS() {}

    /**
     * Search a game tree using open-loop MCTS.
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
        final Node<ACTION> rootNode = new Node<>(numPlayers, null);
        final Random rand = new Random();
        final AtomicInteger iters = new AtomicInteger();
        // -------------------------------

        final Thread[] workers = new Thread[params.threadCount()];
        for(int workerNum = 0; workerNum < workers.length; workerNum++) {
            workers[workerNum] = new Thread(() -> parallelIters(rootState, rootNode, params, rand, iters, start), "olmcts" + workerNum);
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
        for(ACTION action : rootState.availableActions()) {
            final Node<ACTION> child = rootNode.getChild(action);
            final int visitCount = child == null ? 0 : child.getVisitCount();
            if(visitCount > maxVisits) {
                maxVisits = visitCount;
                maxActions.clear();
                maxActions.add(action);
            } else if(visitCount == maxVisits) {
                maxActions.add(action);
            }
        }
        final ACTION optimalAction = maxActions.get(rand.nextInt(maxActions.size()));
        return new SearchResults<>(optimalAction, iters.get(), System.currentTimeMillis() - start);
    }

    private static
    <STATE extends State<STATE, ACTION>, ACTION extends Action<STATE>>
    void parallelIters(STATE rootState, Node<ACTION> rootNode, SearchParameters params, Random rand, AtomicInteger iters, long start) {
        long now = System.currentTimeMillis();
        while(!Thread.interrupted() && now - start <= params.maxTime() && (now - start < params.minTime() || iters.get() <= params.maxIters())) {
            iters.getAndIncrement();
            Node<ACTION> currentNode = rootNode;
            currentNode.threadsSearching.getAndIncrement();

            STATE simulatedState = rootState.copy();
            int playerToMove = simulatedState.playerAboutToMove();
            List<ACTION> availableActions = simulatedState.availableActions();

            // Selection and Expansion - Select child nodes using the tree policy, expanding where necessary.
            boolean continueSelection = true;
            while(!simulatedState.isTerminal() && continueSelection) {
                for(ACTION action : availableActions)
                    currentNode.createChildIfNotPresent(action);

                final ACTION selectedAction = currentNode.selectAction(params.uct(), rand, playerToMove, availableActions);
                Node<ACTION> selectedChild = currentNode.getChild(selectedAction);
                selectedChild.threadsSearching.getAndIncrement();
                if(selectedChild.getVisitCount() == 0)
                    continueSelection = false;

                for(ACTION action : availableActions)
                    currentNode.getChild(action).incAvailableCount();

                simulatedState = selectedAction.applyToState(simulatedState, rand);
                playerToMove = simulatedState.playerAboutToMove();
                availableActions = simulatedState.availableActions();

                currentNode = selectedChild;
            }

            // Simulation - Choose a random action until the game ends.
            while(!simulatedState.isTerminal()) {
                final ACTION action = availableActions.get(rand.nextInt(availableActions.size()));
                simulatedState = action.applyToState(simulatedState, rand);
                availableActions = simulatedState.availableActions();
            }

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            currentNode.backPropagate(simulatedState.scores());

            now = System.currentTimeMillis();
        }
    }
}
