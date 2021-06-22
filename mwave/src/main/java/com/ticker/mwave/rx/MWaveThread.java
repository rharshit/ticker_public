package com.ticker.mwave.rx;

import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.rx.TickerThread;
import com.ticker.mwave.service.MWaveService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.ticker.common.util.Util.WAIT_LONG;
import static com.ticker.common.util.Util.waitFor;

@Getter
@Setter
@Slf4j
@Component("fetcherThread")
@Scope("prototype")
@NoArgsConstructor
public class MWaveThread extends TickerThread<MWaveService> {

    private boolean fetching = false;
    private int currentState;

    private float currentValue;
    private long updatedAt;

    public MWaveThread(ExchangeSymbolEntity entity) {
        super(entity);
    }

    @Override
    public void run() {
        enabled = true;
        initialize();
        if (entity != null) {
            while (!isFetching() && isEnabled()) {
                waitFor(WAIT_LONG);
            }
            while (isEnabled()) {
                while (isEnabled() && isInitialized()) {
                    waitFor(WAIT_LONG);
                }
                if (isEnabled()) {
                    initialize();
                }
            }
        }
        destroy();
    }

    @Override
    public void destroy() {
        service.stopFetching(getExchange(), getSymbol());
    }

    @Override
    public void initialize() {
        initialized = false;
        getService().initializeThread(this);
        initialized = true;
    }

    public String getThreadName() {
        return getExchange() + ":" + getSymbol();
    }

}
