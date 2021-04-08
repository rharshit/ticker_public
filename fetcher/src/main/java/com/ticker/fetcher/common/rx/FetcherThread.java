package com.ticker.fetcher.common.rx;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public abstract class FetcherThread extends Thread {
    private final String threadName;
    private boolean enabled;

    protected FetcherThread(String threadName) {
        this.threadName = threadName;
        this.enabled = true;
    }

    @Override
    public void run() {
        initialize();
        while (enabled) {
            doTask();
        }
        log.info("Terminated thread : " + threadName);
    }

    protected abstract void initialize();

    protected abstract void doTask();

    public void terminateThread() {
        this.enabled = false;
        log.info("Terminating thread : " + threadName);
    }
}
