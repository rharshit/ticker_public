package com.ticker.fetcher.utils;

import java.util.List;

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
    public static double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Double val : values) {
            sum += val;
        }
        return sum / values.size();
    }

    /**
     * Calculate the standard deviation of the values.
     *
     * @param values the values
     * @return the standard deviation
     */
    public static double standardDeviation(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double mean = average(values);
        double sum = 0;
        for (Double val : values) {
            sum += Math.pow(val - mean, 2);
        }
        return Math.sqrt(sum / values.size());
    }
}
