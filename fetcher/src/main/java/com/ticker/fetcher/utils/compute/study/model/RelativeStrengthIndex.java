package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.MathUtil;
import com.ticker.fetcher.utils.compute.ComputeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.ticker.common.util.Util.roundTo2Decimal;

@Slf4j
public class RelativeStrengthIndex extends StudyModel {
    private static final double[] computedValues = new double[1];

    public RelativeStrengthIndex(FetcherThread thread) {
        super(thread);
    }

    @Override
    protected void compute(List<ComputeEngine.ComputeData> values) {
        double[] valsDouble = values.stream().mapToDouble(ComputeEngine.ComputeData::getValue)
                .toArray();
        log.debug("{} - Computing RSI for : ({}) {}", thread.getThreadName(), valsDouble.length, valsDouble);
        int n = valsDouble.length - 1;
        double[] gains = new double[n - 1];
        double[] losses = new double[n - 1];
        for (int i = 1; i < n - 1; i++) {
            double change = valsDouble[i] - valsDouble[i - 1];
            if (change > 0) {
                gains[i] = change;
            } else {
                losses[i] = Math.abs(change);
            }
        }
        double currentChange = valsDouble[n] - valsDouble[n - 1];
        double currentGain = currentChange > 0 ? currentChange : 0;
        double currentLoss = currentChange < 0 ? Math.abs(currentChange) : 0;
        double prevAvgGain = MathUtil.average(gains);
        double prevAvgLoss = MathUtil.average(losses);

        log.trace("{} - Curr Gain: {}", thread.getThreadName(), currentGain);
        log.trace("{} - Curr Loss: {}", thread.getThreadName(), currentLoss);
        log.trace("{} - Prev Gain: {}", thread.getThreadName(), prevAvgGain);
        log.trace("{} - Prev Loss: {}", thread.getThreadName(), prevAvgLoss);
        double rs = ((prevAvgGain * (n - 1)) + currentGain) / ((prevAvgLoss * (n - 1)) + currentLoss);
        log.trace("{} - RS: {}", thread.getThreadName(), rs);
        double rsi = roundTo2Decimal(100 - (100 / (1 + rs)));
        computedValues[0] = roundTo2Decimal(rsi);
        log.debug("{} - Computed RSI: {}", thread.getThreadName(), computedValues[0]);
    }

    @Override
    public double[] getComputedValues() {
        return computedValues;
    }

    @Override
    public void setValues(FetcherThread fetcherThread) {
        fetcherThread.setRsi(computedValues[0]);
    }
}
