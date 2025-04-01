package com.ticker.fetcher.utils;

/**
 * The type Math util.
 */
public class MathUtil {
    private MathUtil() {

    }

    /**
     * Calculate the average of list of values.
     *
     * @param values the values
     * @return the average
     */
    public static double average(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double val : values) {
            sum += val;
        }
        return sum / values.length;
    }

    /**
     * Calculate the standard deviation of the values.
     *
     * @param values the values
     * @return the standard deviation
     */
    public static double standardDeviation(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        double mean = average(values);
        double sum = 0;
        for (Double val : values) {
            sum += Math.pow(val - mean, 2);
        }
        return Math.sqrt(sum / values.length);
    }
}
