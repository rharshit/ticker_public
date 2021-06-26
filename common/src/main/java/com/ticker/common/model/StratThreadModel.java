package com.ticker.common.model;

import com.ticker.common.rx.StratThread;

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

    public long getUpdatedAt() {
        return this.thread.getUpdatedAt();
    }

    public String getUrl() {
        return TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol();
    }

    public int getCurrentState() {
        return this.thread.getCurrentState();
    }

}
