package com.ticker.bbrsisafe.model;

import com.ticker.bbrsisafe.rx.BbRsiSafeThread;
import com.ticker.common.model.StratThreadModel;

public class BbRsiThreadModel extends StratThreadModel<BbRsiSafeThread> {
    public BbRsiThreadModel(BbRsiSafeThread thread) {
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
}
