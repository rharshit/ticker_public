package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.compute.ComputeEngine;

import java.util.LinkedList;

//TODO: Implement this model
public class RelativeStrengthIndex extends StudyModel {
    public RelativeStrengthIndex(FetcherThread thread) {
        super(thread);
    }

    @Override
    public void compute(LinkedList<ComputeEngine.ComputeData> values) {
        thread.setRsi(4);
    }

    @Override
    public double[] getComputedValues() {
        return new double[0];
    }

    @Override
    public void setValues(FetcherThread fetcherThread) {

    }
}
