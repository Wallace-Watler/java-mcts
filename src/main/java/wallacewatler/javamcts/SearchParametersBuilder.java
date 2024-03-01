package wallacewatler.javamcts;

/**
 * Limitations on the duration and iteration count of a tree search. {@code SearchParameters} defines three values:
 * the minimum search time, the maximum search time, and the maximum number of iterations. Search will continue until
 * at least the minimum time has passed, even if it exceeds the maximum number of iterations. After that point, search
 * will stop once either the maximum time has passed or the maximum number of iterations has been exceeded. The actual
 * time spent searching may be larger than the defined maximum time, depending on how long the currently running
 * iteration takes to complete.
 *
 * @version 0.1.0
 * @since 0.1.0
 *
 * @author Wallace Watler
 *
 * @see SearchParameters
 */
public class SearchParametersBuilder {
    private long minTime = 0;
    private long maxTime = Long.MAX_VALUE;
    private int maxIters = 1000;
    private UCT uct = new UCT(Math.sqrt(2), true);
    private int threadCount = 4;

    public SearchParameters build() {
        return new SearchParameters(minTime, maxTime, maxIters, uct, threadCount);
    }

    /**
     * Set the minimum search time. If this is greater than the currently set maximum time, the maximum time will be
     * raised to match the minimum time.
     * @param time the minimum time in milliseconds
     * @return This {@code SearchConstraints}.
     * @throws IllegalArgumentException if the time is less than 0
     */
    public SearchParametersBuilder withMinTime(long time) {
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
     * @throws IllegalArgumentException if the time is less than 0
     */
    public SearchParametersBuilder withMaxTime(long time) {
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
     * @throws IllegalArgumentException if the number of iterations is less than 0
     */
    public SearchParametersBuilder withMaxIters(int iters) {
        if(iters < 0)
            throw new IllegalArgumentException("iterations cannot be negative");

        maxIters = iters;
        return this;
    }

    /**
     * Set the UCT policy.
     * @param uct the UCT policy
     * @return This {@code SearchConstraints}.
     */
    public SearchParametersBuilder withUct(UCT uct) {
        this.uct = uct;
        return this;
    }

    /**
     * Set the number of threads to use during search.
     * @param threadCount the number of threads
     * @return This {@code SearchConstraints}.
     * @throws IllegalArgumentException if the thread count is less than 1
     */
    public SearchParametersBuilder withThreads(int threadCount) {
        if(threadCount <= 0)
            throw new IllegalArgumentException("thread count must be at least 1");

        this.threadCount = threadCount;
        return this;
    }

    @Override
    public String toString() {
        return "SearchParametersBuilder{" +
                "minTime=" + minTime +
                ", maxTime=" + maxTime +
                ", maxIters=" + maxIters +
                ", uct=" + uct +
                ", threadCount=" + threadCount +
                '}';
    }
}
