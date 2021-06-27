package com.ticker.common.rx;

import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.service.StratTickerService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.ticker.common.util.Util.WAIT_LONG;
import static com.ticker.common.util.Util.waitFor;

@Getter
@Setter
@Slf4j
@NoArgsConstructor
public abstract class StratThread<S extends StratTickerService> extends TickerThread<S> {

    private boolean fetching = false;
    private int currentState;

    private float o;
    private float h;
    private float l;
    private float c;
    private float bbU;
    private float bbA;
    private float bbL;
    private float rsi;
    private float currentValue;
    private long updatedAt;


    private float triggerStartValue;
    private long triggerStartTime;
    private int positionQty = 0;

    public StratThread(ExchangeSymbolEntity entity) {
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

    public void setFetchMetrics(Map<String, Object> ticker) {
        if ((Double) ticker.get("currentValue") != 0) {
            setFetching(true);
            setCurrentValue(((Double) ticker.get("currentValue")).floatValue());
            setO(((Double) ticker.get("o")).floatValue());
            setH(((Double) ticker.get("h")).floatValue());
            setL(((Double) ticker.get("l")).floatValue());
            setC(((Double) ticker.get("c")).floatValue());
            setBbU(((Double) ticker.get("bbU")).floatValue());
            setBbA(((Double) ticker.get("bbA")).floatValue());
            setBbL(((Double) ticker.get("bbL")).floatValue());
            setRsi(((Double) ticker.get("rsi")).floatValue());
        } else {
            setFetching(false);
            setCurrentValue(0);
        }
    }

    public void resetTriggers() {
        triggerStartValue = 0;
        triggerStartTime = 0;
    }
}
