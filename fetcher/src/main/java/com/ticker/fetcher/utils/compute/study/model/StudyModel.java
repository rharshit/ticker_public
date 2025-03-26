package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.compute.ComputeEngine;
import lombok.Data;

import java.util.LinkedList;

@Data
public abstract class StudyModel {
    protected final FetcherThread thread;

    public StudyModel(FetcherThread thread) {
        this.thread = thread;
    }

    /**
     * Compute and set values to the thread
     */
    public abstract void compute(LinkedList<ComputeEngine.ComputeData> values);

    /**
     * Return computed values
     * Count of values returned varies
     *
     * @return
     */
    public abstract double[] getComputedValues();

    /**
     * Set computed values
     *
     * @param fetcherThread
     */
    public abstract void setValues(FetcherThread fetcherThread);

}
