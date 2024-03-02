package io.github.wallacewatler.javamcts;

// TODO: Make bestAction an Optional<ACTION>, with NONE if the game has ended
/**
 * Collates the results of a search.
 *
 * @param bestAction The best action found by the search.
 * @param iters The number of iterations performed.
 * @param duration The time taken to complete the search, in milliseconds.
 *
 * @param <ACTION> the type of {@code bestAction}
 */
public record SearchResults<ACTION>(ACTION bestAction, int iters, long duration) {
}
