package com.ticker.mockfetcher.model;

import com.ticker.common.model.TickerThreadModel;
import com.ticker.mockfetcher.common.rx.MockFetcherThread;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;

@NoArgsConstructor
public class FetcherThreadModel extends TickerThreadModel<MockFetcherThread> {

    public FetcherThreadModel(MockFetcherThread thread) {
        super(thread);
    }

    public List<String> getFetcherApps() {
        return Collections.singletonList(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(thread.getStartTime())));
    }

    public float getCurrentValue() {
        return this.thread.getCurrentValue();
    }

    public long getUpdatedAt() {
        return this.thread.getUpdatedAt();
    }

    public String getUrl() {
        return TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol();
    }

    public float getO() {
        return thread.getO();
    }

    public float getH() {
        return thread.getH();
    }

    public float getL() {
        return thread.getL();
    }

    public float getC() {
        return thread.getC();
    }

    public float getBbU() {
        return thread.getBbU();
    }

    public float getBbA() {
        return thread.getBbA();
    }

    public float getBbL() {
        return thread.getBbL();
    }

    public float getRsi() {
        return thread.getRsi();
    }

    public long getDelta() {
        return thread.getDelta();
    }
}
