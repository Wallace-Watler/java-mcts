package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * Multiple-observer information set Monte Carlo tree search (MO-ISMCTS) is effective on games of imperfect information,
 * where knowledge of the game state can vary between players. Each player maintains an information set that represents
 * this knowledge. Furthermore, players do not see other players' actions directly since certain aspects of those
 * actions may be hidden. Instead, players observe actions as moves, which are equivalence classes on actions. It is
 * assumed that every player has full view of their own actions. Simultaneous actions can be modeled as sequential
 * actions that are hidden from all other players until some event reveals them at once. Examples of games that
 * MO-ISMCTS can handle are hearts, cribbage, and poker.
 * <p>
 * To use {@code MOISMCTS}, you'll need to implement four interfaces: {@link State}, {@link ObservableAction},
 * {@link InfoSet}, and {@link Move}. You can then perform the search by calling {@link MOISMCTS#search} on one of the
 * provided {@code MOISMCTS} implementations (see below).
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MOISMCTSRP Root-parallelized MO-ISMCTS
 * @see MOISMCTSTP Tree-parallelized MO-ISMCTS
 */
public interface MOISMCTS {
    /**
     * Perform MO-ISMCTS from a given player's information set. {@code rand} is used for both MO-ISMCTS itself and for
     * game mechanics. {@code rand} can be seeded to reproduce single-threaded runs from a particular information set.
     *
     * @apiNote Trees are not reused between searches because players do not observe other players' information sets or
     * moves, and therefore cannot update other players' trees whenever an action is applied to the true game state.
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
     * @see SearchParameters
     * @see SearchResults
     */
    <STATE extends State<ACTION>, ACTION extends ObservableAction<STATE, MOVE>, MOVE extends Move<ACTION>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<STATE, MOVE> infoSet, SearchParameters params, Random rand);
}
