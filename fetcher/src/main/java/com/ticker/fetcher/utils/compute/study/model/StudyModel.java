package com.ticker.fetcher.utils.compute.study.model;

import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.compute.ComputeEngine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public abstract class StudyModel {
    protected final FetcherThread thread;

    public StudyModel(FetcherThread thread) {
        this.thread = thread;
    }

    /**
     * Compute and set values to the thread
     */
    public void compute(List<ComputeEngine.ComputeData> values, int windowSize) {
        if (values.size() == windowSize) {
            log.trace("{} : Computing values", thread.getThreadName());
            compute(values);
        }
    }

    protected abstract void compute(List<ComputeEngine.ComputeData> values);


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
