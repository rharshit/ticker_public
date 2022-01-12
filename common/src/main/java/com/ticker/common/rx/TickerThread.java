package com.ticker.common.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.service.TickerThreadService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Getter
@Setter
@Slf4j
@NoArgsConstructor
public abstract class TickerThread<S extends TickerThreadService> extends Thread {

    protected boolean enabled = false;
    protected boolean initialized = false;
    protected boolean locked = false;

    protected S service;
    protected ExchangeSymbolEntity entity;

    public TickerThread(ExchangeSymbolEntity entity) {
        this.entity = entity;
    }

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

    protected abstract void initialize();

    public abstract String getThreadName();

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

    public void terminateThread() {
        this.enabled = false;
        log.info(getThreadName() + " : terminating thread");
    }

    public String getExchange() {
        return entity.getExchangeId();
    }

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
