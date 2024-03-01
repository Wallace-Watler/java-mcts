package wallacewatler.javamcts;

/**
 * @param optimalAction the best action found by the search
 * @param iters the number of iterations performed
 * @param duration the time taken to complete the search, in milliseconds
 *
 * @param <ACTION> the type of {@code optimalAction}
 */
public record SearchResults<ACTION>(ACTION optimalAction, int iters, long duration) {
}
