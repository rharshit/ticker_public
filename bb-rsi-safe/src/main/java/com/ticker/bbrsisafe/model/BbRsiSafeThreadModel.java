package com.ticker.bbrsisafe.model;

import com.ticker.bbrsisafe.rx.BbRsiSafeThread;
import com.ticker.common.model.StratThreadModel;

public class BbRsiSafeThreadModel extends StratThreadModel<BbRsiSafeThread> {
    public BbRsiSafeThreadModel(BbRsiSafeThread thread) {
        super(thread);
    }

    public float getDip() {
        return thread.getDip();
    }

    public float getPeak() {
        return thread.getPeak();
    }

    public boolean isSafeState() {
        return thread.getPositionQty() == 0;
    }

    public boolean isLowTarget() {
        return thread.isLowValue();
    }

    public float getTradeValue() {
        return thread.getTradeValue();
    }

    public float getTrailValue() {
        return thread.getTrailValue();
    }
}
