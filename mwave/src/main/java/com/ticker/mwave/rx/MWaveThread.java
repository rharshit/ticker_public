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

import static com.ticker.common.util.Util.*;

@Getter
@Setter
@Slf4j
@Component("fetcherThread")
@Scope("prototype")
@NoArgsConstructor
public class MWaveThread extends TickerThread<MWaveService> {

    private boolean fetching = false;

    public MWaveThread(ExchangeSymbolEntity entity) {
        super(entity);
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

    @Override
    public void destroy() {

    }

    @Override
    protected void initialize() {
        fetching = true;
        enabled = true;
        initialized = true;
    }

    public String getThreadName() {
        return getExchange() + ":" + getSymbol();
    }

}
