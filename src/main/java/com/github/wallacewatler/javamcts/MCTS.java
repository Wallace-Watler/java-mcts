package com.github.wallacewatler.javamcts;

import java.util.Random;

/**
 * This is the classic form of Monte Carlo tree search, closed loop MCTS, and is effective on deterministic games of
 * perfect information. The entire game state and all players' actions are visible to everyone, and every action has a
 * pre-determined effect on the state. Examples of games in this category are Tic-Tac-Toe (a.k.a. Naughts and Crosses),
 * chess, and mancala.
 * <p>
 * To use {@code MCTS}, you'll need to implement two interfaces: {@link VisibleState} and {@link DeterministicAction}.
 * You can then perform the search by calling {@link MCTS#search} on one of the provided {@code MCTS} implementations
 * (see below).
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see MCTSRP Root-parallelized MCTS
 * @see MCTSTP Tree-parallelized MCTS
 */
public interface MCTS {
    /**
     * Perform MCTS from a given state. {@code rootState} must have valid actions to perform the search. {@code rand} is
     * only used for the algorithm itself, not for any game mechanics. {@code rand} can be seeded to reproduce
     * single-threaded runs from a particular state.
     * <p>
     * If a transposition table is used, the {@code STATE} type should have {@link Object#equals} and
     * {@link Object#hashCode} defined as its objects will be used as keys in a {@link java.util.HashMap HashMap}.
     *
     * @param numPlayers the number of players in the game
     * @param rootState the state from which to begin the search
     * @param params the search parameters
     * @param rand a source of randomness
     * @param useTable whether to use a transposition table during the search
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
    <STATE extends VisibleState<STATE, ACTION>, ACTION extends DeterministicAction<STATE>>
    SearchResults<ACTION> search(int numPlayers, STATE rootState, SearchParameters params, Random rand, boolean useTable);
}
