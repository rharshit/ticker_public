package com.ticker.mwave.model;

import com.ticker.common.model.TickerThreadModel;
import com.ticker.mwave.rx.MWaveThread;

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
}
