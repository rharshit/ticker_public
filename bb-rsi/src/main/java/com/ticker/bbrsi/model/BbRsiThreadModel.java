package com.ticker.bbrsi.model;

import com.ticker.bbrsi.rx.BbRsiThread;
import com.ticker.common.model.StratThreadModel;

public class BbRsiThreadModel extends StratThreadModel<BbRsiThread> {
    public BbRsiThreadModel(BbRsiThread thread) {
        super(thread);
    }

    public float getDip() {
        return thread.getDip();
    }

    public float getPeak() {
        return thread.getPeak();
    }
}
