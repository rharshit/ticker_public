package com.ticker.mwave.model;

import com.ticker.common.model.TickerThreadModel;
import com.ticker.mwave.rx.MWaveThread;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;

public class MWaveThreadModel extends TickerThreadModel<MWaveThread> {
    public MWaveThreadModel(MWaveThread thread) {
        super(thread);
    }

    public boolean isFetching() {
        return this.thread.isFetching();
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

}
