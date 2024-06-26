package com.github.wallacewatler.javamcts;

/**
 * Collates the results of a search.
 *
 * @param bestAction The best action found by the search.
 * @param itersPerThread The average number of iterations performed per thread.
 * @param duration The time taken to complete the search, in milliseconds.
 * @param numNodes The number of nodes searched.
 * @param numStates The number of unique states encountered. If a transposition table is not used, this will be 0.
 *
 * @param <ACTION> the type of {@code bestAction}
 */
public record SearchResults<ACTION>(
        ACTION bestAction,
        double itersPerThread,
        long duration,
        int numNodes,
        int numStates
) {}
