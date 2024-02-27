package wallacewatler.moismcts;

/**
 * Limitations on the duration and iteration count of a tree search. {@code SearchParameters} defines three values:
 * the minimum search time, the maximum search time, and the maximum number of iterations. Search will continue until
 * at least the minimum time has passed, even if it exceeds the maximum number of iterations. After that point, search
 * will stop once either the maximum time has passed or the maximum number of iterations has been exceeded. The actual
 * time spent searching may be larger than the defined maximum time, depending on how long the currently running
 * iteration takes to complete.
 *
 * @param minTime
 * @param maxTime
 * @param maxIters
 * @param uct
 * @param threadCount
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 */
public record SearchParameters(long minTime, long maxTime, int maxIters, UCT uct, int threadCount) {
    public SearchParameters {
        if(minTime < 0)
            throw new IllegalArgumentException("time cannot be negative");

        if(minTime > maxTime)
            throw new IllegalArgumentException("the minimum time cannot be larger than the maximum time");

        if(maxIters < 0)
            throw new IllegalArgumentException("iterations cannot be negative");

        if(threadCount < 1)
            throw new IllegalArgumentException("thread count must be at least 1");
    }
}
