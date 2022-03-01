package com.ticker.common.model;

import com.ticker.common.rx.StratThread;

import java.util.Map;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;

/**
 * The type Strat thread model.
 *
 * @param <T> the type parameter
 */
public class StratThreadModel<T extends StratThread> extends TickerThreadModel<T> {
    /**
     * Instantiates a new Strat thread model.
     *
     * @param thread the thread
     */
    public StratThreadModel(T thread) {
        super(thread);
    }

    /**
     * Is fetching boolean.
     *
     * @return the boolean
     */
    public boolean isFetching() {
        return this.thread.isFetching();
    }

    /**
     * Gets ticker type.
     *
     * @return the ticker type
     */
    public String getTickerType() {
        return this.thread.getTickerType();
    }

    /**
     * Gets current value.
     *
     * @return the current value
     */
    public double getCurrentValue() {
        return this.thread.getCurrentValue();
    }

    /**
     * Gets o.
     *
     * @return the o
     */
    public double getO() {
        return thread.getO();
    }

    /**
     * Gets h.
     *
     * @return the h
     */
    public double getH() {
        return thread.getH();
    }

    /**
     * Gets l.
     *
     * @return the l
     */
    public double getL() {
        return thread.getL();
    }

    /**
     * Gets c.
     *
     * @return the c
     */
    public double getC() {
        return thread.getC();
    }

    /**
     * Gets bb u.
     *
     * @return the bb u
     */
    public double getBbU() {
        return thread.getBbU();
    }

    /**
     * Gets bb a.
     *
     * @return the bb a
     */
    public double getBbA() {
        return thread.getBbA();
    }

    /**
     * Gets bb l.
     *
     * @return the bb l
     */
    public double getBbL() {
        return thread.getBbL();
    }

    /**
     * Gets rsi.
     *
     * @return the rsi
     */
    public double getRsi() {
        return thread.getRsi();
    }

    /**
     * Gets tema.
     *
     * @return the tema
     */
    public double getTema() {
        return thread.getTema();
    }

    /**
     * Gets position qty.
     *
     * @return the position qty
     */
    public double getPositionQty() {
        return thread.getPositionQty();
    }

    /**
     * Gets updated at.
     *
     * @return the updated at
     */
    public long getUpdatedAt() {
        return this.thread.getUpdatedAt();
    }

    /**
     * Gets url.
     *
     * @return the url
     */
    public String getUrl() {
        return TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol();
    }

    /**
     * Gets current state.
     *
     * @return the current state
     */
    public int getCurrentState() {
        return this.thread.getCurrentState();
    }

    /**
     * Gets target threshold.
     *
     * @return the target threshold
     */
    public double getTargetThreshold() {
        return this.thread.getTargetThreshold();
    }

    /**
     * Gets state trace.
     *
     * @return the state trace
     */
    public Map<Long, Integer> getStateTrace() {
        return this.thread.getStateTrace();
    }

}
