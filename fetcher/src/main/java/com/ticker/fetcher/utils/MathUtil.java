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

    /**
     * Calculate the Exponential Moving Average (EMA) of the values.
     *
     * @param values
     * @return
     */
    public static double[] ema(double[] values) {
        if (values.length == 0) {
            return new double[0];
        }
        double[] emas = new double[values.length];
        double multiplier = 2.0 / (values.length + 1);
        double ema = values[0];
        emas[0] = ema;
        for (int i = 1; i < values.length; i++) {
            ema = ((values[i] - ema) * multiplier) + ema;
            emas[i] = ema;
        }
        return emas;
    }
}
