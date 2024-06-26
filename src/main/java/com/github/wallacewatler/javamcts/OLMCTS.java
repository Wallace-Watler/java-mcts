package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * <h3>Open-loop Monte Carlo tree search (OLMCTS)</h3>
 * OLMCTS is effective on stochastic games of perfect information, games involving non-discrete states, and games of
 * hidden information where said information is hidden from all players. An action may involve randomness such that it
 * can lead to one of many possible states. The revealing of hidden information can be treated as a random event.
 * <p>
 * To use {@code OLMCTS}, you'll need to implement two interfaces: {@link VisibleState} and {@link Action}. You can then
 * perform the search by calling {@link OLMCTS#search} on one of the provided {@code OLMCTS} implementations.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see OLMCTSRP
 * @see OLMCTSTP
 */
public interface OLMCTS {
    /**
     * Perform OLMCTS from a given game state. {@code rand} is used for both OLMCTS itself and for game mechanics.
     * {@code rand} can be seeded to reproduce single-threaded runs from a particular initial state. The tree is
     * discarded once search is complete.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state from which to begin the search
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The search results.
     *
     * @throws IllegalArgumentException if {@code numPlayers} is less than 1
     *
     * @param <STATE> the type of state the search operates on
     * @param <ACTION> the type of action the search operates on
     *
     * @apiNote Trees are not reused between searches because information is gained after applying an action to the true
     * game state; previous searches operated without that knowledge (assuming the true state was updated since the last
     * search) and therefore may not be reflective of the true game state's value.
     *
     * @see SearchParameters
     * @see SearchResults
     */
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends Action<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand);

    /**
     * <p>
     *     Actions are chosen by players and applied to states to progress the game. An {@code OLMCTS.Action} may
     *     involve randomness.
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
         * Compute and return the result of applying this action to {@code state}. {@code rand} should be used to
         * compute stochastic game mechanics. {@code state} may be mutated by this method. If {@code state} is an
         * object, it is recommended that {@code state} is mutated into the resulting state and returned as it will
         * likely run faster than creating a new object.
         *
         * @param state the state to apply this action to
         * @param rand a source of randomness
         *
         * @return The result of applying this action to a state.
         */
        STATE applyToState(STATE state, Random rand);
    }
}
