package com.ticker.mockfetcher.model;

import com.ticker.common.model.TickerThreadModel;
import com.ticker.mockfetcher.rx.MockFetcherThread;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;

/**
 * The type Fetcher thread model.
 */
@NoArgsConstructor
public class MockFetcherThreadModel extends TickerThreadModel<MockFetcherThread> {

    /**
     * Instantiates a new Fetcher thread model.
     *
     * @param thread the thread
     */
    public MockFetcherThreadModel(MockFetcherThread thread) {
        super(thread);
    }

    /**
     * Gets fetcher apps.
     *
     * @return the fetcher apps
     */
    public List<String> getFetcherApps() {
        return Collections.singletonList(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(thread.getStartTime())));
    }

    /**
     * Gets current value.
     *
     * @return the current value
     */
    public float getCurrentValue() {
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
    public float getO() {
        return thread.getO();
    }

    /**
     * Gets h.
     *
     * @return the h
     */
    public float getH() {
        return thread.getH();
    }

    /**
     * Gets l.
     *
     * @return the l
     */
    public float getL() {
        return thread.getL();
    }

    /**
     * Gets c.
     *
     * @return the c
     */
    public float getC() {
        return thread.getC();
    }

    /**
     * Gets bb u.
     *
     * @return the bb u
     */
    public float getBbU() {
        return thread.getBbU();
    }

    /**
     * Gets bb a.
     *
     * @return the bb a
     */
    public float getBbA() {
        return thread.getBbA();
    }

    /**
     * Gets bb l.
     *
     * @return the bb l
     */
    public float getBbL() {
        return thread.getBbL();
    }

    /**
     * Gets rsi.
     *
     * @return the rsi
     */
    public float getRsi() {
        return thread.getRsi();
    }

    /**
     * Gets tema.
     *
     * @return the tema
     */
    public float getTema() {
        return thread.getTema();
    }

    /**
     * Gets day o.
     *
     * @return the day o
     */
    public float getDayO() {
        return thread.getDayO();
    }

    /**
     * Gets day h.
     *
     * @return the day h
     */
    public float getDayH() {
        return thread.getDayH();
    }

    /**
     * Gets day l.
     *
     * @return the day l
     */
    public float getDayL() {
        return thread.getDayL();
    }

    /**
     * Gets day c.
     *
     * @return the day c
     */
    public float getDayC() {
        return thread.getDayC();
    }

    /**
     * Gets prev close.
     *
     * @return the prev close
     */
    public float getPrevClose() {
        return thread.getPrevClose();
    }

    /**
     * Gets delta.
     *
     * @return the delta
     */
    public long getDelta() {
        return thread.getDelta();
    }
}
