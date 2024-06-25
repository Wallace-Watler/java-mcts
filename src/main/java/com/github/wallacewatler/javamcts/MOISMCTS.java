package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * <h3>Multiple-observer information set Monte Carlo tree search (MO-ISMCTS)</h3>
 * <p>
 *     MO-ISMCTS is effective on games of imperfect information, where knowledge of the game state can vary between
 *     players. Each player maintains an information set that represents this knowledge. Furthermore, players do not
 *     observe other players' actions directly since certain aspects of those actions may be hidden. Instead, players
 *     observe moves, which are equivalence classes on actions. It is assumed that every player has full view of their
 *     own actions. Simultaneous actions can be modeled as sequential actions that are hidden from all other players
 *     until some event reveals them at once. Examples of games that MO-ISMCTS can handle are hearts, cribbage, and
 *     poker.
 * </p>
 * <p>
 *     To use {@code MOISMCTS}, you'll need to implement four interfaces: {@link State}, {@link Action},
 *     {@link InfoSet}, and {@link Move}. You can then perform the search by calling {@link MOISMCTS#search} on one of
 *     the provided {@code MOISMCTS} implementations.
 * </p>
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MOISMCTSRP
 * @see MOISMCTSTP
 */
public interface MOISMCTS {
    /**
     * Perform MO-ISMCTS from a given player's information set. {@code rand} is used for both MO-ISMCTS itself and for
     * game mechanics. {@code rand} can be seeded to reproduce single-threaded runs from a particular information set.
     * The tree is discarded once search is complete.
     *
     * @param numPlayers the number of players in the game
     * @param infoSet the information set of the player doing the search
     * @param params the search parameters
     * @param rand a source of randomness
     *
     * @return The search results.
     *
     * @throws IllegalArgumentException if {@code numPlayers} is less than 1
     *
     * @param <STATE> the type of state this search will operate on
     * @param <ACTION> the type of action this search will operate on
     * @param <MOVE> the type of move this search will operate on
     *
     * @apiNote Trees are not reused between searches because players do not observe other players' information sets or
     * moves, and therefore cannot update other players' trees whenever an action is applied to the true game state.
     *
     * @see SearchParameters
     * @see SearchResults
     */
    <STATE extends State<ACTION>, ACTION extends Action<STATE, MOVE>, MOVE extends Move<ACTION>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<STATE, MOVE> infoSet, SearchParameters params, Random rand);

    /**
     * <p>
     *     Actions are chosen by players and applied to states to progress the game. {@code MOISMCTS.Action} represents
     *     an action in which randomness may be involved and aspects of the action may be partially or entirely hidden
     *     from other players. It is possible that two different actions are indistinguishable from a certain player's
     *     point of view, so actions are said to be observed by players as moves.
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
     * @param <MOVE> the type of move this action produces when observed by a player
     *
     * @see State
     * @see Move
     */
    interface Action<STATE, MOVE> {
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

        /**
         * @param observer the player observing this action
         *
         * @return A move representing this action as observed from {@code observer}'s point of view.
         * If {@code observer} is the player performing this action, the returned move must represent full knowledge of
         * this action (i.e. the move must be a singleton).
         */
        MOVE observe(int observer);
    }
}
