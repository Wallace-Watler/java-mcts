package wallacewatler.moismcts;

import java.util.*;

/**
 * Multiple-observer information set Monte Carlo tree search (MO-ISMCTS).
 * <p>
 * MO-ISMCTS is applicable to games of imperfect information, where knowledge of the game state can vary between
 * players. Each player maintains a so-called information set that represents this knowledge. Furthermore, players do
 * not observe other players' actions directly since certain aspects of those actions may be hidden. Instead, players
 * observe moves, which are a player's view of an action. It is assumed that every player has full view of their own
 * actions. Players cannot observe other players' information sets; all knowledge is gained through observing moves.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @see State
 * @see Action
 * @see InfoSet
 * @see Move
 * @see MOISMCTS#search
 *
 * @author Wallace Watler
 */
public final class MOISMCTS {
    /**
     * Search a game tree using MO-ISMCTS.
     *
     * @param numPlayers the number of players in the game
     * @param infoSet the information set of the player doing the search
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The optimal action.
     *
     * @param <STATE> the type of state this search will operate on
     * @param <ACTION> the type of action this search will operate on
     * @param <INFO_SET> the type of information set this search will operate on
     * @param <MOVE> the type of move this search will operate on
     *
     * @see State
     * @see Action
     * @see InfoSet
     * @see Move
     */
    public static
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, INFO_SET extends InfoSet<STATE, ACTION>, MOVE extends Move<ACTION, INFO_SET>>
    ACTION search(int numPlayers, INFO_SET infoSet, SearchParameters params, Random rand) {
        final int playerSearching = infoSet.playerId();

        if(playerSearching >= numPlayers)
            throw new IllegalArgumentException("player ID must be less than the number of players, given ID " + playerSearching + " and " + numPlayers + " players");

        // The root node for each player's tree. The trees are shared across threads.
        final ArrayList<Node<STATE, MOVE>> rootNodes = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            rootNodes.add(new Node<>());

        final Thread[] threads = new Thread[params.threadCount()];
        for(int threadNum = 0; threadNum < params.threadCount(); threadNum++) {
            // Divide iterations as evenly as possible between the threads.
            final int threadIters = params.maxIters() / params.threadCount() + (params.maxIters() % params.threadCount() > threadNum ? 1 : 0);

            // Each thread gets its own RNG so that its results are reproducible.
            final Random threadRand = new Random(rand.nextLong());

            threads[threadNum] = new Thread(() -> parallelIters(numPlayers, infoSet, params, threadRand, rootNodes, threadIters), "moismcts" + threadNum);
            threads[threadNum].start();
        }

        for(Thread thread : threads) {
            try {
                thread.join();
            } catch(InterruptedException ignored) {}
        }

        // Recommend the most selected action. Ties are broken by randomness.
        final Node<STATE, MOVE> rootNode = rootNodes.get(playerSearching);
        final ArrayList<ACTION> maxActions = new ArrayList<>();
        int maxVisits = 0;
        for(ACTION action : infoSet.determinize(rand).availableActions()) {
            final int visitCount = rootNode.child(action.observe(playerSearching)).getVisitCount();
            if(visitCount > maxVisits) {
                maxVisits = visitCount;
                maxActions.clear();
                maxActions.add(action);
            } else if(visitCount == maxVisits) {
                maxActions.add(action);
            }
        }

        return maxActions.get(rand.nextInt(maxActions.size()));
    }

    private static
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, INFO_SET extends InfoSet<STATE, ACTION>, MOVE extends Move<ACTION, INFO_SET>>
    void parallelIters(int numPlayers, INFO_SET infoSet, SearchParameters params, Random rand, ArrayList<Node<STATE, MOVE>> rootNodes, int threadIters) {
        final int playerSearching = infoSet.playerId();
        final long start = System.currentTimeMillis();

        // Pre-allocated list used during iteration
        final ArrayList<ACTION> availableActions = new ArrayList<>();

        // The path through each player's tree
        final ArrayList<ArrayDeque<Node<STATE, MOVE>>> traversedNodes = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            traversedNodes.add(new ArrayDeque<>());

        int iters = 0;
        long now = System.currentTimeMillis();
        while(now - start <= params.maxTime() && (now - start < params.minTime() || iters <= threadIters)) {
            // Begin iteration at the root
            for(int i = 0; i < numPlayers; i++) {
                traversedNodes.get(i).clear();
                traversedNodes.get(i).push(rootNodes.get(i));
            }

            // Choose a random determinized state consistent with the information set of the player searching the tree.
            STATE simulatedState = infoSet.determinize(rand);

            // Selection and Expansion - Select child nodes using the tree policy, expanding where necessary.
            availableActions.clear();
            availableActions.addAll(simulatedState.availableActions());
            infoSet.removePoorActions(availableActions);
            Node<STATE, MOVE> currentNode = rootNodes.get(playerSearching);

            boolean continueSelection = true;
            while(!simulatedState.isTerminal() && continueSelection) {
                final int playerAboutToMove = simulatedState.playerAboutToMove();

                final ACTION selectedAction = currentNode.chooseAction(playerAboutToMove, availableActions, rand, params.uct());
                for(ACTION action : availableActions)
                    currentNode.child(action.observe(playerAboutToMove)).incAvailableCount();

                final Node<STATE, MOVE> selectedChild = currentNode.child(selectedAction.observe(playerAboutToMove));
                if(selectedChild.getVisitCount() == 0)
                    continueSelection = false;

                /*
                A node's visit count is updated as soon as it's selected so that it has a virtual loss (its UCT value
                drops) until backpropagation updates its total reward. This is important for parallelization as it
                lessens the chance that many threads choose the same path simultaneously.
                 */
                selectedChild.incVisitCount();

                simulatedState = selectedAction.applyToState(simulatedState, rand);

                // Descend through each player's tree.
                for(int i = 0; i < numPlayers; i++)
                    traversedNodes.get(i).push(currentNode.child(selectedAction.observe(i)));

                availableActions.clear();
                availableActions.addAll(simulatedState.availableActions());
                currentNode = traversedNodes.get(simulatedState.playerAboutToMove()).getFirst();
            }

            // Simulation - Choose a random action until the games ends.
            while(!simulatedState.isTerminal()) {
                final ACTION action = availableActions.get(rand.nextInt(availableActions.size()));
                simulatedState = action.applyToState(simulatedState, rand);
                availableActions.clear();
                availableActions.addAll(simulatedState.availableActions());
            }

            final double[] rewards = simulatedState.rewards();

            // Backpropagation - Update all nodes that were selected with the results of simulation.
            for(int i = 0; i < numPlayers; i++)
                for(Node<STATE, MOVE> node : traversedNodes.get(i))
                    node.addReward(rewards[i]);

            iters++;
            now = System.currentTimeMillis();
        }
    }
}
