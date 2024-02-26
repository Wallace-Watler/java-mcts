package wallacewatler.moismcts;

/**
 * Limitations on the duration and iteration count of a tree search. {@code SearchConstraints} defines three values:
 * the minimum search time, the maximum search time, and the maximum number of iterations. Search will continue until
 * at least the minimum time has passed, even if it exceeds the maximum number of iterations. After that point, search
 * will stop once either the maximum time has passed or the maximum number of iterations has been exceeded. The actual
 * time spent searching may be larger than the defined maximum time, depending on how long the currently running
 * iteration takes to complete.
 *
 * @since 0.1
 *
 * @author Wallace Watler
 */
public class SearchConstraints {
    private long minTime = 0;
    private long maxTime = Long.MAX_VALUE;
    private int maxIters = 1000;

    /**
     * @return The minimum time in milliseconds.
     */
    public long getMinTime() {
        return minTime;
    }

    /**
     * @return The maximum time in milliseconds.
     */
    public long getMaxTime() {
        return maxTime;
    }

    /**
     * @return The maximum number of iterations.
     */
    public int getMaxIters() {
        return maxIters;
    }

    /**
     * Set the minimum search time. If this is greater than the currently set maximum time, the maximum time will be
     * raised to match the minimum time.
     * @param time the minimum time in milliseconds
     * @return This {@code SearchConstraints}.
     */
    public SearchConstraints withMinTime(long time) {
        if(time < 0)
            throw new IllegalArgumentException("time cannot be negative");

        minTime = time;
        maxTime = Math.max(minTime, maxTime);
        return this;
    }

    /**
     * Set the maximum search time. If this is less than the currently set minimum time, the minimum time will be
     * lowered to match the maximum time.
     * @param time the maximum time in milliseconds
     * @return This {@code SearchConstraints}.
     */
    public SearchConstraints withMaxTime(long time) {
        if(time < 0)
            throw new IllegalArgumentException("time cannot be negative");

        maxTime = time;
        minTime = Math.min(minTime, maxTime);
        return this;
    }

    /**
     * Set the maximum number of iterations.
     * @param iters the maximum number of iterations
     * @return This {@code SearchConstraints}.
     */
    public SearchConstraints withMaxIters(int iters) {
        if(iters < 0)
            throw new IllegalArgumentException("iterations cannot be negative");

        maxIters = iters;
        return this;
    }

    @Override
    public String toString() {
        return "SearchConstraints{" +
                "minTime=" + minTime +
                ", maxTime=" + maxTime +
                ", maxIters=" + maxIters +
                '}';
    }
}
