package com.ticker.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticker.common.rx.TickerThread;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TickerThreadModel<T extends TickerThread> {

    @JsonIgnore
    protected T thread;

    public String getThreadName() {
        return this.thread.getThreadName();
    }

    public boolean isEnabled() {
        return this.thread.isEnabled();
    }

    public boolean isInitialized() {
        return this.thread.isInitialized();
    }

    public String getExchange() {
        return this.thread.getExchange();
    }

    public String getSymbol() {
        return this.thread.getSymbol();
    }

}
