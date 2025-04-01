package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.MathUtil;
import com.ticker.fetcher.utils.compute.ComputeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.ticker.fetcher.utils.MathUtil.roundTo2Decimal;

@Slf4j
public class BollingerBands extends StudyModel {
    private static final double[] computedValues = new double[3];

    public BollingerBands(FetcherThread thread) {
        super(thread);
    }

    @Override
    protected void compute(List<ComputeEngine.ComputeData> values) {
        double[] valsDouble = values.stream().mapToDouble(ComputeEngine.ComputeData::getValue)
                .toArray();
        log.debug("{} - Computing Bollinger Bands for : {}", thread.getThreadName(), valsDouble);

        double average = MathUtil.average(valsDouble);
        log.trace("{} - Average: {}", thread.getThreadName(), average);
        double stdDev = MathUtil.standardDeviation(valsDouble);
        log.trace("{} - Standard Deviation: {}", thread.getThreadName(), stdDev);
        double upperBand = average + (stdDev * 2);
        log.trace("{} - Upper Band: {}", thread.getThreadName(), upperBand);
        double lowerBand = average - (stdDev * 2);
        log.trace("{} - Lower Band: {}", thread.getThreadName(), lowerBand);
        computedValues[0] = roundTo2Decimal(average);
        computedValues[1] = roundTo2Decimal(upperBand);
        computedValues[2] = roundTo2Decimal(lowerBand);
        log.debug("{} - Computed Bollinger Bands: {}", thread.getThreadName(), computedValues);
    }

    @Override
    public double[] getComputedValues() {
        return computedValues;
    }

    @Override
    public void setValues(FetcherThread fetcherThread) {
        fetcherThread.setBbA(computedValues[0]);
        fetcherThread.setBbU(computedValues[1]);
        fetcherThread.setBbL(computedValues[2]);
    }
}
