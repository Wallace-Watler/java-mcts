package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * Information set Monte Carlo tree search (ISMCTS) is effective on games of imperfect information, where knowledge of
 * the game state can vary between players. Each player maintains an information set that represents this knowledge. As
 * actions are done, knowledge may be gained from them.
 * <p>
 * To use {@code ISMCTS}, you'll need to implement three interfaces: {@link State}, {@link StochasticAction}, and
 * {@link InfoSet}. You can then perform the search by calling {@link ISMCTS#search} on one of the provided
 * {@code ISMCTS} implementations (see below). The {@code MOVE} type parameter of your {@code InfoSet} implementation
 * should be the type of your {@code StochasticAction}. This is because {@code ISMCTS} is a special case of
 * {@link MOISMCTS} where all actions are fully visible to all players, hence moves and actions are one and the same.
 *
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see ISMCTSRP Root-parallelized ISMCTS
 * @see ISMCTSTP Tree-parallelized ISMCTS
 */
public interface ISMCTS {
    /**
     * Perform ISMCTS from a given player's information set. {@code rand} is used for both ISMCTS itself and for game
     * mechanics. {@code rand} can be seeded to reproduce single-threaded runs from a particular information set.
     *
     * @apiNote The search tree is constructed and discarded within this method. Although classic MCTS may benefit from
     * reusing trees, the tree is not kept here because information about the true game state may be gained whenever an
     * action is done, rendering any previous search tree non-representative of the game.
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
     *
     * @see SearchParameters
     * @see SearchResults
     */
    <STATE extends State<ACTION>, ACTION extends StochasticAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, InfoSet<STATE, ACTION> infoSet, SearchParameters params, Random rand);
}
