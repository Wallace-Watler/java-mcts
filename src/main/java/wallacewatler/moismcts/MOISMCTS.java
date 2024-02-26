package wallacewatler.moismcts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multiple-observer information set Monte Carlo tree search (MO-ISMCTS).
 * <p>
 * MO-ISMCTS is applicable to games of imperfect information, where knowledge of the game state can vary between
 * players.
 * <p>
 * In MO-ISMCTS, players do not observe other players' actions directly since certain aspects of those actions may be
 * hidden. Instead, players observe moves, which represent a player's knowledge of an action. It is assumed that every
 * player has full knowledge of their own actions.
 * <p>
 * Players cannot observe other players' information sets. They can observe part or all of the game state and part or
 * all of each player's actions.
 *
 * @since 0.1
 *
 * @see State
 * @see Action
 * @see InfoSet
 * @see Move
 *
 * @author Wallace Watler
 *
 * @param <STATE> the type of state this tree search operates on
 * @param <ACTION> the type of action this tree search operates on
 * @param <INFO_SET> the type of information set this tree search operates on
 * @param <MOVE> the type of move this tree search operates on
 */
public final class MOISMCTS<STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, INFO_SET extends InfoSet<STATE, MOVE>, MOVE extends Move<ACTION, INFO_SET>> {
    /** The number of players in the game. */
    public final int numPlayers;

    /**
     * The true game state. This should not be touched while a game is in progress; doing so will cause undefined
     * behavior. If you need to apply an action to {@code rootState}, use {@link #advanceRootState(Action, Random)}.
     */
    public STATE rootState;

    /**
     * The UCT policy to use for the selection phase.
     * @see UCT
     */
    public final UCT uctPolicy;
    public final SimulationPolicy<MOVE> simulationPolicy;

    private final ArrayList<PlayerPerspective<STATE, INFO_SET, MOVE>> perspectives;

    // Pre-allocated list used during iteration
    private final ArrayList<MOVE> availableActions = new ArrayList<>();

    public MOISMCTS(STATE state, List<INFO_SET> infoSets, UCT uctPolicy, SimulationPolicy<MOVE> simulationPolicy) {
        if(infoSets.size() == 0)
            throw new IllegalArgumentException("MCTS requires at least 1 player but was given none");

        this.numPlayers = infoSets.size();
        rootState = state;
        perspectives = new ArrayList<>(numPlayers);
        for(int i = 0; i < numPlayers; i++)
            perspectives.add(new PlayerPerspective<>(infoSets.get(i)));

        this.uctPolicy = uctPolicy;
        this.simulationPolicy = simulationPolicy;
    }

    /**
     * Get the optimal action to play based on previous searches. Ties are broken by random selection.
     * @param rand a source of randomness
     * @return The optimal action.
     */
    public ACTION recommendAction(Random rand) {
        final int player = rootState.playerAboutToMove();
        final Node<STATE, MOVE> rootNode = perspectives.get(player).getRootNode();
        final ArrayList<ACTION> maxActions = new ArrayList<>();
        final AtomicInteger maxVisits = new AtomicInteger(0);

        for(ACTION action : rootState.availableActions()) {
            final int visitCount = rootNode.child(action.observe(player)).visitCount;
            if(visitCount > maxVisits.get()) {
                maxVisits.set(visitCount);
                maxActions.clear();
                maxActions.add(action);
            } else if(visitCount == maxVisits.get()) {
                maxActions.add(action);
            }
        }

        return maxActions.get(rand.nextInt(maxActions.size()));
    }

    /**
     *
     * @param action the action to apply to the root state
     * @param rand a source of randomness
     */
    public void advanceRootState(ACTION action, Random rand) {
        rootState = action.applyToState(rootState, rand);
        for(int i = 0; i < numPlayers; i++)
            perspectives.get(i).advanceRoot(action.observe(i));
    }

    /**
     *
     * @param constraints limits on the search time and the number of iterations
     * @param rand a source of randomness
     */
    public void search(SearchConstraints constraints, Random rand) {
        int iters = 0;
        final long start = System.currentTimeMillis();
        long now = start;
        while(now - start <= constraints.getMaxTime() && (now - start < constraints.getMinTime() || iters <= constraints.getMaxIters())) {
            iterate(rand);
            iters++;
            now = System.currentTimeMillis();
        }
    }

    private void iterate(Random rand) {
        for(PlayerPerspective<STATE, INFO_SET, MOVE> perspective : perspectives)
            perspective.beginTraversal();

        // Choose a random determinized state consistent with the information set of the player searching the tree.
        PlayerPerspective<STATE, INFO_SET, MOVE> playerToMove = perspectives.get(rootState.playerAboutToMove());
        STATE simulatedState = playerToMove.getInfoSet().determinize(rand);

        // Selection and Expansion - Select child nodes using the tree policy, expanding where necessary.
        Node<STATE, MOVE> currentNode = playerToMove.getRootNode();
        setAvailableActions(simulatedState);
        playerToMove.getInfoSet().removePoorMoves(availableActions);

        boolean continueSelection = true;
        while(!simulatedState.isTerminal() && continueSelection) {
            final MOVE selectedMove = uctPolicy.chooseMove(currentNode, availableActions, rand);
            for(MOVE action : availableActions)
                currentNode.child(action).availableCount++;

            // If node resulting from selected move has not been visited, stop selection.
            if(currentNode.child(selectedMove).visitCount == 0)
                continueSelection = false;

            final ACTION selectedAction = selectedMove.asAction();
            simulatedState = selectedAction.applyToState(simulatedState, rand);

            // Descend through each player's tree.
            for(int i = 0; i < numPlayers; i++)
                perspectives.get(i).descend(selectedAction.observe(i));

            playerToMove = perspectives.get(simulatedState.playerAboutToMove());
            setAvailableActions(simulatedState);
            currentNode = playerToMove.currentNode();
        }

        // Simulation - Use the simulation policy to run until the end of the game.
        while(!simulatedState.isTerminal()) {
            final MOVE move = simulationPolicy.chooseMove(availableActions, rand);
            simulatedState = move.asAction().applyToState(simulatedState, rand);
            setAvailableActions(simulatedState);
        }

        final double[] rewards = simulatedState.rewards();

        // Backpropagation - Update all nodes that were selected with the results of simulation.
        for(int i = 0; i < numPlayers; i++)
            perspectives.get(i).backPropagate(rewards[i]);
    }

    private void setAvailableActions(STATE state) {
        final int playerAboutToMove = state.playerAboutToMove();
        availableActions.clear();
        for(ACTION action : state.availableActions())
            availableActions.add(action.observe(playerAboutToMove));
    }
}
