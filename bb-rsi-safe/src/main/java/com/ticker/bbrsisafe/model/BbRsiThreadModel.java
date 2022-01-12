package com.ticker.bbrsisafe.model;

import com.ticker.bbrsisafe.rx.BbRsiThread;
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

    public boolean isSafeState() {
        return thread.isSafeState();
    }

    public boolean isLowTarget() {
        return thread.isLowValue();
    }
}
