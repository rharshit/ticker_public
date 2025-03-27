package com.ticker.fetcher.model;

import com.ticker.common.model.TickerThreadModel;
import com.ticker.fetcher.rx.FetcherThread;
import lombok.NoArgsConstructor;

import java.util.Set;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;

/**
 * The type Fetcher thread model.
 */
@NoArgsConstructor
public class FetcherThreadModel extends TickerThreadModel<FetcherThread> {

    /**
     * Instantiates a new Fetcher thread model.
     *
     * @param thread the thread
     */
    public FetcherThreadModel(FetcherThread thread) {
        super(thread);
    }

    public boolean isPrevDataPopulated() {
        return this.thread.isPrevDataPopulated();
    }

    /**
     * Gets fetcher apps.
     *
     * @return the fetcher apps
     */
    public Set<String> getFetcherApps() {
        return this.thread.getFetcherApps();
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
     * Gets last ping at.
     *
     * @return the last ping at
     */
    public long getLastPingAt() {
        return thread.getLastPingAt();
    }

    /**
     * Gets day o.
     *
     * @return the day o
     */
    public double getDayO() {
        return thread.getDayO();
    }

    /**
     * Gets day h.
     *
     * @return the day h
     */
    public double getDayH() {
        return thread.getDayH();
    }

    /**
     * Gets day l.
     *
     * @return the day l
     */
    public double getDayL() {
        return thread.getDayL();
    }

    /**
     * Gets day c.
     *
     * @return the day c
     */
    public double getDayC() {
        return thread.getDayC();
    }

    /**
     * Gets prev close.
     *
     * @return the prev close
     */
    public double getPrevClose() {
        return thread.getPrevClose();
    }
}
