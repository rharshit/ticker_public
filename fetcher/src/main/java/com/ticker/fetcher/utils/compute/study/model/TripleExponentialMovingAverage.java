package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.compute.ComputeEngine;

import java.util.List;

//TODO: Implement this model
public class TripleExponentialMovingAverage extends StudyModel {
    public TripleExponentialMovingAverage(FetcherThread thread) {
        super(thread);
    }

    @Override
    protected void compute(List<ComputeEngine.ComputeData> values) {
        thread.setTema(5);
    }

    @Override
    public double[] getComputedValues() {
        return new double[0];
    }

    @Override
    public void setValues(FetcherThread fetcherThread) {

    }
}
