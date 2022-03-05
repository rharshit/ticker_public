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

/**
 * The type Strat thread.
 *
 * @param <S> the type parameter
 */
@Getter
@Setter
@Slf4j
@NoArgsConstructor
public abstract class StratThread<S extends StratTickerService> extends TickerThread<S> {

    /**
     * The State trace.
     */
    Map<Long, Integer> stateTrace = new HashMap<>();
    private boolean fetching = false;
    private int currentState;
    private double o;
    private double h;
    private double l;
    private double c;
    private double bbU;
    private double bbA;
    private double bbL;
    private double rsi;
    private double tema;
    private double dayO;
    private double dayH;
    private double dayL;
    private double dayC;
    private double prevClose;
    private double currentValue;
    private long updatedAt;
    private String tickerType = "";
    private double triggerStartValue;
    private long triggerStartTime;
    private int positionQty = 0;

    private double targetThreshold;
    private boolean lowValue = false;

    /**
     * Instantiates a new Strat thread.
     *
     * @param entity the entity
     */
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
            setTargetThreshold(0.0006 * getCurrentValue());
            StratThread thisThread = this;
            Thread thread = new Thread(() -> service.setTargetThreshold(thisThread));
            thread.start();
            while (isEnabled()) {
                while (isEnabled() && isInitialized()) {
                    waitFor(WAIT_LONG);
                }
                if (isEnabled()) {
                    initialize();
                }
            }
        }
        terminateThread(false);
    }

    @Override
    public void terminateThread(boolean shutdownInitiates) {
        super.terminateThread(shutdownInitiates);
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

    /**
     * Sets fetch metrics.
     *
     * @param ticker the ticker
     */
    public void setFetchMetrics(Map<String, Object> ticker) {
        if ((Double) ticker.get("currentValue") != 0) {
            setFetching(true);
            setCurrentValue((double) ticker.get("currentValue"));
            setO((double) ticker.get("o"));
            setH((double) ticker.get("h"));
            setL((double) ticker.get("l"));
            setC((double) ticker.get("c"));
            setBbU((double) ticker.get("bbU"));
            setBbA((double) ticker.get("bbA"));
            setBbL((double) ticker.get("bbL"));
            setRsi((double) ticker.get("rsi"));
            setTema((double) ticker.get("tema"));
            setDayO((double) ticker.get("dayO"));
            setDayH((double) ticker.get("dayH"));
            setDayL((double) ticker.get("dayL"));
            setDayC((double) ticker.get("dayC"));
            setPrevClose((double) ticker.get("prevClose"));
            if (getO() * getH() * getL() * getC() * getBbL() * getBbA() * getBbU() * getRsi() * getTema()
                    * getDayO() * getDayH() * getDayL() * getDayC() * getPrevClose() == 0) {
                setFetching(false);
                setCurrentValue(0);
            } else {
                setUpdatedAt((Long) ticker.get("updatedAt"));
            }
        } else {
            setFetching(false);
            setCurrentValue(0);
        }
        log.trace(getThreadName() + ":");
        log.trace(getThreadName() + ":" + getCurrentValue());
        log.trace(getThreadName() + ":");
        log.trace(getThreadName() + ":" + getO());
        log.trace(getThreadName() + ":" + getH());
        log.trace(getThreadName() + ":" + getL());
        log.trace(getThreadName() + ":" + getC());
        log.trace(getThreadName() + ":" + getBbL());
        log.trace(getThreadName() + ":" + getBbA());
        log.trace(getThreadName() + ":" + getBbU());
        log.trace(getThreadName() + ":" + getRsi());
        log.trace(getThreadName() + ":" + getTema());
        log.trace(getThreadName() + ":" + getDayO());
        log.trace(getThreadName() + ":" + getDayH());
        log.trace(getThreadName() + ":" + getDayL());
        log.trace(getThreadName() + ":" + getDayC());
        log.trace(getThreadName() + ":" + getPrevClose());
    }

    /**
     * Sets current state.
     *
     * @param currentState the current state
     */
    public void setCurrentState(int currentState) {
        log.info(getThreadName() + " : change state");
        log.info(getThreadName() + " : from " + getCurrentState() + " : " + service.getStateValueMap().get(getCurrentState()));
        this.currentState = currentState;
        log.info(getThreadName() + " : to   " + getCurrentState() + " : " + service.getStateValueMap().get(getCurrentState()));
        stateTrace.put(System.currentTimeMillis(), getCurrentState());
    }

    /**
     * Sets intermediate state.
     *
     * @param intermediateState the intermediate state
     */
    public void setIntermediateState(int intermediateState) {
        setStates(intermediateState, getCurrentState());
    }

    /**
     * Sets intermediate state.
     *
     * @param intermediateState the intermediate state
     * @param finalState        the final state
     */
    public void setIntermediateState(int intermediateState, int finalState) {
        setStates(intermediateState, finalState);
    }

    /**
     * Sets states.
     *
     * @param states the states
     */
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

    /**
     * Reset triggers.
     */
    public void resetTriggers() {
        triggerStartValue = 0;
        triggerStartTime = 0;
    }

    /**
     * Sets target threshold.
     *
     * @param targetThreshold the target threshold
     */
    public void setTargetThreshold(double targetThreshold) {
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
