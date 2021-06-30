package com.ticker.common.model;

import com.ticker.common.rx.StratThread;

import java.util.Map;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;

public class StratThreadModel<T extends StratThread> extends TickerThreadModel<T> {
    public StratThreadModel(T thread) {
        super(thread);
    }

    public boolean isFetching() {
        return this.thread.isFetching();
    }

    public float getCurrentValue() {
        return this.thread.getCurrentValue();
    }

    public float getO() {
        return thread.getO();
    }

    public float getH() {
        return thread.getH();
    }

    public float getL() {
        return thread.getL();
    }

    public float getC() {
        return thread.getC();
    }

    public float getBbU() {
        return thread.getBbU();
    }

    public float getBbA() {
        return thread.getBbA();
    }

    public float getBbL() {
        return thread.getBbL();
    }

    public float getRsi() {
        return thread.getRsi();
    }

    public long getUpdatedAt() {
        return this.thread.getUpdatedAt();
    }

    public String getUrl() {
        return TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol();
    }

    public int getCurrentState() {
        return this.thread.getCurrentState();
    }

    public float getTargetThreshold() {
        return this.thread.getTargetThreshold();
    }

    public Map<Long, Integer> getStateTrace() {
        return this.thread.getStateTrace();
    }

}
