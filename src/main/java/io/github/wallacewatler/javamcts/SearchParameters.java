package io.github.wallacewatler.javamcts;

/**
 * {@code SearchParameters} defines limitations on the duration and iteration count of a tree search: the minimum search
 * time, the maximum search time, and the maximum number of iterations. Search will continue until at least the minimum
 * time has passed, even if it exceeds the maximum number of iterations. After that point, search will stop once either
 * the maximum time has passed or the maximum number of iterations has been reached. The actual time spent searching may
 * be larger than the defined maximum time, depending on how long the currently running iterations take to complete.<br>
 * <br>
 * {@code SearchParameters} also defines the number of threads to use for parallelized search (or serial search in the
 * case of one thread). There is no guarantee that more threads will result in more efficient searches.
 *
 * @param minTime The minimum search time in milliseconds.
 * @param maxTime The maximum search time in milliseconds.
 * @param maxIters The maximum number of iterations.
 * @param uct The UCT policy.
 * @param threadCount The number of threads to use for the search.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see UCT
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
