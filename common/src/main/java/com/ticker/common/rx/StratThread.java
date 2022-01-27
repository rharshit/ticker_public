package com.ticker.common.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.service.StratTickerService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
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
    private float tema;
    private float currentValue;
    private long updatedAt;
    private String tickerType = "";

    Map<Long, Integer> stateTrace = new HashMap<>();

    private float triggerStartValue;
    private long triggerStartTime;
    private int positionQty = 0;

    private float targetThreshold;
    private boolean lowValue = false;

    public StratThread(ExchangeSymbolEntity entity) {
        super(entity);
    }

    @Override
    public void run() {
        enabled = true;
        log.info("CP1");
        initialize();
        log.info("CP2");
        if (entity != null) {
            log.info("CP3");
            while (!isFetching() && isEnabled()) {
                log.info("CP4");
                waitFor(WAIT_LONG, this);
                log.info("CP5");
            }
            log.info("CP6");
            setTargetThreshold(0.0006f * getCurrentValue());
            service.setTargetThreshold(this);
            log.info("CP7");
            while (isEnabled()) {
                log.info("CP8");
                while (isEnabled() && isInitialized()) {
                    log.info("CP9");
                    waitFor(WAIT_LONG, this);
                    log.info("CP10");
                }
                log.info("CP11");
                if (isEnabled()) {
                    log.info("CP12");
                    initialize();
                    log.info("CP13");
                }
                log.info("CP14");
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
            setTema(((Double) ticker.get("tema")).floatValue());
            if (getO() * getH() * getL() * getC() * getBbL() * getBbA() * getBbU() * getRsi() == 0) {
                setFetching(false);
                setCurrentValue(0);
            } else {
                setUpdatedAt((Long) ticker.get("updatedAt"));
            }
        } else {
            setFetching(false);
            setCurrentValue(0);
        }
        log.debug("");
        log.debug(String.valueOf(getCurrentValue()));
        log.debug("");
        log.debug(String.valueOf(getO()));
        log.debug(String.valueOf(getH()));
        log.debug(String.valueOf(getL()));
        log.debug(String.valueOf(getC()));
        log.debug(String.valueOf(getBbL()));
        log.debug(String.valueOf(getBbA()));
        log.debug(String.valueOf(getBbU()));
        log.debug(String.valueOf(getRsi()));
    }

    public void setCurrentState(int currentState) {
        log.info(getThreadName() + " : change state");
        log.info(getThreadName() + " : from " + getCurrentState() + " : " + service.getStateValueMap().get(getCurrentState()));
        this.currentState = currentState;
        log.info(getThreadName() + " : to   " + getCurrentState() + " : " + service.getStateValueMap().get(getCurrentState()));
        stateTrace.put(System.currentTimeMillis(), getCurrentState());
    }

    public void setIntermediateState(int intermediateState) {
        setStates(intermediateState, getCurrentState());
    }

    public void setIntermediateState(int intermediateState, int finalState) {
        setStates(intermediateState, finalState);
    }

    public void setStates(int... states) {
        for (int state : states) {
            setCurrentState(state);
            try {
                sleep(5);
            } catch (InterruptedException e) {
                log.error(e.getLocalizedMessage());
            }
        }
    }

    public void resetTriggers() {
        triggerStartValue = 0;
        triggerStartTime = 0;
    }

    public void setTargetThreshold(float targetThreshold) {
        if (targetThreshold < 0.055f) {
            this.targetThreshold = 0.045f;
            this.lowValue = true;
        } else if (targetThreshold < 0.105f) {
            this.targetThreshold = 0.095f;
            this.lowValue = true;
        } else {
            this.targetThreshold = targetThreshold;
        }
    }
}
