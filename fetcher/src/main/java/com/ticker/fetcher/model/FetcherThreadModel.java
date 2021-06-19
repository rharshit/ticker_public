package com.ticker.fetcher.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticker.fetcher.common.rx.FetcherThread;
import lombok.Data;

import java.util.Set;

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
}
