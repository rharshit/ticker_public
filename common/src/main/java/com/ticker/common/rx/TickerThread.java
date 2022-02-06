package com.ticker.common.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.service.TickerThreadService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * The type Ticker thread.
 *
 * @param <S> the type parameter
 */
@Getter
@Setter
@Slf4j
@NoArgsConstructor
public abstract class TickerThread<S extends TickerThreadService> extends Thread {

    /**
     * The Enabled.
     */
    protected boolean enabled = false;
    /**
     * The Initialized.
     */
    protected boolean initialized = false;
    /**
     * The Locked.
     */
    protected boolean locked = false;

    /**
     * The Service.
     */
    protected S service;
    /**
     * The Entity.
     */
    protected ExchangeSymbolEntity entity;

    {
        setDaemon(true);
    }

    /**
     * Instantiates a new Ticker thread.
     *
     * @param entity the entity
     */
    public TickerThread(ExchangeSymbolEntity entity) {
        this.entity = entity;
    }

    /**
     * Create compare object ticker thread.
     *
     * @param entity the entity
     * @return the ticker thread
     */
    public static TickerThread createCompareObject(ExchangeSymbolEntity entity) {
        return new TickerThread(entity) {
            @Override
            protected void initialize() {

            }

            @Override
            public String getThreadName() {
                return null;
            }

            @Override
            public void run() {

            }

            @Override
            public void destroy() {

            }
        };
    }

    /**
     * Initialize.
     */
    protected abstract void initialize();

    /**
     * Gets thread name.
     *
     * @return the thread name
     */
    public abstract String getThreadName();

    /**
     * Sets entity.
     *
     * @param entity the entity
     */
    public void setEntity(ExchangeSymbolEntity entity) {
        if (entity == null) {
            throw new TickerException(getThreadName() + " : No entity found for the given exchange and symbol");
        }
        this.entity = entity;
    }

    @Override
    public abstract void run();

    @Override
    public abstract void destroy();

    /**
     * Terminate thread.
     */
    public void terminateThread() {
        this.enabled = false;
        log.info(getThreadName() + " : terminating thread");
    }

    /**
     * Gets exchange.
     *
     * @return the exchange
     */
    public String getExchange() {
        return entity.getExchangeId();
    }

    /**
     * Gets symbol.
     *
     * @return the symbol
     */
    public String getSymbol() {
        return entity.getSymbolId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TickerThread)) return false;
        TickerThread<?> that = (TickerThread<?>) o;
        return Objects.equals(getExchange(), that.getExchange()) &&
                Objects.equals(getSymbol(), that.getSymbol());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExchange(), getSymbol());
    }

    /**
     * The type Comparator.
     */
    public static class Comparator implements java.util.Comparator<TickerThread> {

        @Override
        public int compare(TickerThread o1, TickerThread o2) {
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (o2 == null) {
                    return -1;
                }
            }
            if (o1.getExchange().compareTo(o2.getExchange()) == 0) {
                return o1.getSymbol().compareTo(o2.getSymbol());
            } else {
                return o1.getExchange().compareTo(o2.getExchange());
            }
        }
    }
}
