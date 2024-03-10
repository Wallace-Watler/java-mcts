package com.github.wallacewatler.javamcts;

/**
 * Useful for computing the mean and sample standard deviation of a set in an online fashion. This uses Welford's online
 * algorithm for doing so, where individual samples are not stored.
 */
public class Sampler {
    private double total = 0.0;
    private double mean = 0.0;
    private double m2 = 0.0;
    private int count = 0;

    /**
     * @return The number of samples received so far.
     */
    public int getCount() {
        return count;
    }

    /**
     * @return The sum of the samples received so far.
     */
    public double getTotal() {
        return total;
    }

    /**
     * @return The arithmetic mean of the samples received so far.
     */
    public double getMean() {
        return mean;
    }

    /**
     * @return The sample variance of the samples received so far.
     */
    public synchronized double getVariance() {
        return count < 2 ? Double.POSITIVE_INFINITY : m2 / (count - 1);
    }

    /**
     * @return The sample standard deviation of the samples received so far.
     */
    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    /**
     * @param sample a value to submit to this sampler
     */
    public synchronized void addSample(double sample) {
        total += sample;
        count++;
        final double delta = sample - mean;
        mean += delta / count;
        final double delta2 = sample - mean;
        m2 += delta * delta2;
    }
}
