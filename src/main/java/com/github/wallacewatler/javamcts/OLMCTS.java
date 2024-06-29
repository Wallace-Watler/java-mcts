package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * Open-loop Monte Carlo tree search (OLMCTS) is effective on stochastic games of perfect information, games involving
 * non-discrete states, and games of hidden information where said information is hidden from all players. An action may
 * involve randomness such that it can lead to one of many possible states.
 * <p>
 * To use {@code OLMCTS}, you'll need to implement two interfaces: {@link VisibleState} and {@link StochasticAction}.
 * You can then perform the search by calling {@link OLMCTS#search} on one of the provided {@code OLMCTS}
 * implementations (see below). To handle information that is hidden from all players, you can treat its reveal as a
 * random event; that is, you can bake it into {@code StochasticAction}. To handle information that is only hidden from
 * some players, use {@link MOISMCTS}.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see OLMCTSRP Root-parallelized OLMCTS
 * @see OLMCTSTP Tree-parallelized OLMCTS
 */
public interface OLMCTS {
    /**
     * Perform OLMCTS from a given game state. {@code rand} is used for both OLMCTS itself and for game mechanics.
     * {@code rand} can be seeded to reproduce single-threaded runs from a particular initial state.
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
     * @see SearchParameters
     * @see SearchResults
     */
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends StochasticAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand);
}
