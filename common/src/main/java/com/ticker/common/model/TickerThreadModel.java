package com.ticker.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ticker.common.rx.TickerThread;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Ticker thread model.
 *
 * @param <T> the type parameter
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TickerThreadModel<T extends TickerThread> {

    /**
     * The Thread.
     */
    @JsonIgnore
    protected T thread;

    /**
     * Gets thread name.
     *
     * @return the thread name
     */
    public String getThreadName() {
        return this.thread.getThreadName();
    }

    /**
     * Is enabled boolean.
     *
     * @return the boolean
     */
    public boolean isEnabled() {
        return this.thread.isEnabled();
    }

    /**
     * Is initialized boolean.
     *
     * @return the boolean
     */
    public boolean isInitialized() {
        return this.thread.isInitialized();
    }

    public boolean isRefreshing() {
        return this.thread.isRefreshing();
    }

    /**
     * Gets exchange.
     *
     * @return the exchange
     */
    public String getExchange() {
        return this.thread.getExchange();
    }

    /**
     * Gets symbol.
     *
     * @return the symbol
     */
    public String getSymbol() {
        return this.thread.getSymbol();
    }

}
