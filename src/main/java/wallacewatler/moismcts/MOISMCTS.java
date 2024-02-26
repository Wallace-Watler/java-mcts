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
        final int player = infoSet.playerId();

        if(player >= numPlayers)
            throw new IllegalArgumentException("player ID must be less than the number of players, given ID " + player + " and " + numPlayers + " players");

        final ArrayList<PlayerPerspective<STATE, INFO_SET, MOVE>> perspectives = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            perspectives.add(new PlayerPerspective<>());

        // Pre-allocated list used during iteration
        final ArrayList<ACTION> availableActions = new ArrayList<>();

        int iters = 0;
        final long start = System.currentTimeMillis();
        long now = start;
        while(now - start <= params.getMaxTime() && (now - start < params.getMinTime() || iters <= params.getMaxIters())) {
            // Begin iteration
            for(PlayerPerspective<STATE, INFO_SET, MOVE> perspective : perspectives)
                perspective.beginTraversal();

            // Choose a random determinized state consistent with the information set of the player searching the tree.
            PlayerPerspective<STATE, INFO_SET, MOVE> playerToMove = perspectives.get(player);
            STATE simulatedState = infoSet.determinize(rand);

            // Selection and Expansion - Select child nodes using the tree policy, expanding where necessary.
            Node<STATE, MOVE> currentNode = playerToMove.getRootNode();
            availableActions.clear();
            availableActions.addAll(simulatedState.availableActions());
            infoSet.removePoorActions(availableActions);

            boolean continueSelection = true;
            while(!simulatedState.isTerminal() && continueSelection) {
                final int playerAboutToMove = simulatedState.playerAboutToMove();

                final ACTION selectedAction = params.getUctPolicy().chooseAction(playerAboutToMove, currentNode, availableActions, rand);
                for(ACTION action : availableActions)
                    currentNode.child(action.observe(playerAboutToMove)).availableCount++;

                // If node resulting from selected move has not been visited, stop selection.
                if(currentNode.child(selectedAction.observe(playerAboutToMove)).visitCount == 0)
                    continueSelection = false;

                simulatedState = selectedAction.applyToState(simulatedState, rand);

                // Descend through each player's tree.
                for(int i = 0; i < perspectives.size(); i++)
                    perspectives.get(i).descend(selectedAction.observe(i));

                playerToMove = perspectives.get(simulatedState.playerAboutToMove());
                availableActions.clear();
                availableActions.addAll(simulatedState.availableActions());
                currentNode = playerToMove.currentNode();
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
            for(int i = 0; i < perspectives.size(); i++)
                perspectives.get(i).backPropagate(rewards[i]);

            iters++;
            now = System.currentTimeMillis();
        }

        // Recommend the most selected action. Ties are broken by randomness.
        final Node<STATE, MOVE> rootNode = perspectives.get(player).getRootNode();
        final ArrayList<ACTION> maxActions = new ArrayList<>();
        int maxVisits = 0;
        for(ACTION action : infoSet.determinize(rand).availableActions()) {
            final int visitCount = rootNode.child(action.observe(player)).visitCount;
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
}
