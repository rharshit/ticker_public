package com.ticker.fetcher.model;

import com.ticker.common.model.TickerThreadModel;
import com.ticker.fetcher.rx.FetcherThread;
import lombok.NoArgsConstructor;

import java.util.Set;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;

@NoArgsConstructor
public class FetcherThreadModel extends TickerThreadModel<FetcherThread> {

    public FetcherThreadModel(FetcherThread thread) {
        super(thread);
    }

    public Set<String> getFetcherApps() {
        return this.thread.getFetcherApps();
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

    public float getTema() {
        return thread.getTema();
    }
}
