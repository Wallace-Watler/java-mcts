package io.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * <b>Closed loop Monte Carlo tree search</b><br>
 * This is classic MCTS, effective on deterministic games of perfect information. The entire game state and all players'
 * actions are visible to everyone, and every action has a pre-determined effect on the state.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MCTSRP
 * @see MCTSTP
 * @see VisibleState
 * @see Action
 */
public interface MCTS {
    /**
     * Perform MCTS from a given state. The root state must have valid actions to perform the search. {@code rand} is
     * only used for the algorithm itself, not for any game mechanics. {@code rand} can be seeded to reproduce
     * single-threaded runs from a particular state.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state from which to begin search
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The search results.
     *
     * @throws IllegalArgumentException if {@code numPlayers} is less than 1
     *
     * @param <STATE> the type of state this MCTS operates on
     * @param <ACTION> the type of action this MCTS operates on
     *
     * @see SearchParameters
     * @see SearchResults
     */
    <STATE extends VisibleState<STATE, ? extends ACTION>, ACTION extends Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand);

    /**
     * Actions are chosen by players and applied to states to progress the game. An {@code MCTS.Action} does not involve
     * any randomness and is fully visible to everyone, so it is completely predictable.
     *
     * @version 0.1.0
     * @since 0.1.0
     *
     * @author Wallace Watler
     *
     * @param <STATE> the type of state this action applies to
     */
    interface Action<STATE> {
        /**
         * Compute and return the result of applying this action to {@code state}. {@code state} may be mutated by this
         * method.
         *
         * @param state the state to apply this action to
         *
         * @return The result of applying this action to a state.
         *
         * @implNote If {@code state} is an object, it is recommended that {@code state} is mutated into the resulting
         * state and returned as it will likely run faster than creating a new object.
         */
        STATE applyToState(STATE state);
    }
}
