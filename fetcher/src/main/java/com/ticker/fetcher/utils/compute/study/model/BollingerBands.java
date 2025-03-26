package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.compute.ComputeEngine;

import java.util.LinkedList;

//TODO: Implement this model
public class BollingerBands extends StudyModel {
    public BollingerBands(FetcherThread thread) {
        super(thread);
    }

    @Override
    public void compute(LinkedList<ComputeEngine.ComputeData> values) {
        thread.setBbL(values.stream().mapToDouble(ComputeEngine.ComputeData::getValue).min().orElse(1));
        thread.setBbA(values.stream().mapToDouble(ComputeEngine.ComputeData::getValue).average().orElse(1));
        thread.setBbU(values.stream().mapToDouble(ComputeEngine.ComputeData::getValue).max().orElse(3));
    }

    @Override
    public double[] getComputedValues() {
        return new double[0];
    }

    @Override
    public void setValues(FetcherThread fetcherThread) {

    }
}
