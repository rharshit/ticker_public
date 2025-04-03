package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.MathUtil;
import com.ticker.fetcher.utils.compute.ComputeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.ticker.common.util.Util.roundTo2Decimal;

@Slf4j
public class TripleExponentialMovingAverage extends StudyModel {
    private static final double[] computedValues = new double[1];

    public TripleExponentialMovingAverage(FetcherThread thread) {
        super(thread);
    }

    @Override
    protected void compute(List<ComputeEngine.ComputeData> values) {
        double[] valsDouble = values.stream().mapToDouble(ComputeEngine.ComputeData::getValue)
                .toArray();
        log.debug("{} - Computing TEMA for : {}", thread.getThreadName(), valsDouble);
        double[] ema1 = MathUtil.ema(valsDouble);
        log.trace("{} - EMA1 : {}", thread.getThreadName(), ema1);
        double[] ema2 = MathUtil.ema(ema1);
        log.trace("{} - EMA2 : {}", thread.getThreadName(), ema2);
        double[] ema3 = MathUtil.ema(ema2);
        log.trace("{} - EMA3 : {}", thread.getThreadName(), ema3);

        double tema = roundTo2Decimal(3 * (ema1[ema1.length - 1] - ema2[ema2.length - 1]) + ema3[ema3.length - 1]);
        computedValues[0] = tema;
        log.debug("{} - TEMA: {}", thread.getThreadName(), tema);
    }

    @Override
    public double[] getComputedValues() {
        return computedValues;
    }

    @Override
    public void setValues(FetcherThread fetcherThread) {
        thread.setTema(computedValues[0]);
    }
}
