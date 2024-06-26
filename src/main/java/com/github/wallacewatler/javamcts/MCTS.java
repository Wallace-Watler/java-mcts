package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * <h3>Closed Loop Monte Carlo tree search</h3>
 * This is classic MCTS, effective on deterministic games of perfect information. The entire game state and all players'
 * actions are visible to everyone, and every action has a pre-determined effect on the state. Examples of games in this
 * category are Tic-Tac-Toe, chess, and mancala.
 * <p>
 * To use {@code MCTS}, you'll need to implement two interfaces: {@link VisibleState} and {@link Action}. You can then
 * perform the search by calling {@link MCTS#search} on one of the provided {@code MCTS} implementations.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MCTSRP
 * @see MCTSTP
 */
public interface MCTS {
    /**
     * Perform MCTS from a given state. {@code rootState} must have valid actions to perform the search. {@code rand} is
     * only used for the algorithm itself, not for any game mechanics. {@code rand} can be seeded to reproduce
     * single-threaded runs from a particular state. The tree is discarded once search is complete.
     * <p>
     * If a transposition table is used, the {@code STATE} type should have {@link Object#equals} and
     * {@link Object#hashCode} defined as its objects will be used as keys in a {@link java.util.HashMap HashMap}.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state from which to begin the search
     * @param params the search parameters
     * @param rand a source of randomness
     * @param useTTable whether to use a transposition table during the search
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
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand, boolean useTTable);

    /**
     * <p>
     *     Actions are chosen by players and applied to states to progress the game. An {@code MCTS.Action} does not
     *     involve any randomness and is fully visible to everyone, so it is completely predictable.
     * </p>
     * <p>
     *     Implementations of {@code Action} should include {@link Object#equals} and {@link Object#hashCode} since
     *     actions are internally used as keys in hashmaps.
     * </p>
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
         * method. If {@code state} is an object, it is recommended that {@code state} is mutated into the resulting
         * state and returned as it will likely run faster than creating a new object.
         *
         * @param state the state to apply this action to
         *
         * @return The result of applying this action to a state.
         */
        STATE applyToState(STATE state);
    }
}
