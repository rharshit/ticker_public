package com.ticker.fetcher.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticker.fetcher.common.rx.FetcherThread;
import lombok.Data;

import java.util.Set;

import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_CHART;

@Data
public class FetcherThreadModel {
    @JsonIgnore
    private final FetcherThread fetcherThread;

    public FetcherThreadModel(FetcherThread fetcherThread) {
        this.fetcherThread = fetcherThread;
    }

    public String getThreadName() {
        return this.fetcherThread.getThreadName();
    }

    public boolean isEnabled() {
        return this.fetcherThread.isEnabled();
    }

    public boolean isInitialized() {
        return this.fetcherThread.isInitialized();
    }

    public String getExchange() {
        return this.fetcherThread.getExchange();
    }

    public String getSymbol() {
        return this.fetcherThread.getSymbol();
    }

    public Set<String> getFetcherApps() {
        return this.fetcherThread.getFetcherApps();
    }

    public float getCurrentValue() {
        return this.fetcherThread.getCurrentValue();
    }

    public long getUpdatedAt() {
        return this.fetcherThread.getUpdatedAt();
    }

    public String getUrl() {
        return TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol();
    }

    public float getO() {
        return fetcherThread.getO();
    }

    public float getH() {
        return fetcherThread.getH();
    }

    public float getL() {
        return fetcherThread.getL();
    }

    public float getC() {
        return fetcherThread.getC();
    }

    public float getBbU() {
        return fetcherThread.getBbU();
    }

    public float getBbA() {
        return fetcherThread.getBbA();
    }

    public float getBbL() {
        return fetcherThread.getBbL();
    }

    public float getRsi() {
        return fetcherThread.getRsi();
    }
}
