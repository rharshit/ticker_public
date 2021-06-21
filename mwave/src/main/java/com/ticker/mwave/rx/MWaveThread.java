package com.ticker.mwave.rx;

import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.mwave.service.MWaveService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.ticker.common.util.Util.*;

@Getter
@Setter
@Slf4j
@Component("fetcherThread")
@Scope("prototype")
@NoArgsConstructor
public class MWaveThread extends Thread {

    private boolean fetching = false;
    private boolean enabled = false;
    private boolean initialized = false;

    private MWaveService mWaveService;
    private ExchangeSymbolEntity entity;

    public MWaveThread(ExchangeSymbolEntity entity) {
        this.entity = entity;
    }

    @Override
    public void run() {
        initialize();
        if (entity != null) {
            while (!fetching) {
                waitFor(WAIT_LONG);
            }
            while (isEnabled()) {
                while (isEnabled() && isInitialized()) {
                    waitFor(WAIT_SHORT);
                }
            }
        }
        destroy();
    }

    // TODO: Implement initializing
    private void initialize() {
        fetching = true;
        enabled = true;
        initialized = true;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    public String getExchange() {
        return entity.getExchangeId();
    }

    public String getSymbol() {
        return entity.getSymbolId();
    }

    public void terminateThread() {
        this.enabled = false;
        log.info("Terminating thread : " + getThreadName());
    }

    public String getThreadName() {
        return getExchange() + ":" + getSymbol();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MWaveThread thread = (MWaveThread) o;
        return getExchange().equals(thread.getExchange()) &&
                getSymbol().equals(thread.getSymbol());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExchange(), getSymbol());
    }

    public static class Comparator implements java.util.Comparator<MWaveThread> {

        @Override
        public int compare(MWaveThread o1, MWaveThread o2) {
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
