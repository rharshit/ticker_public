package com.ticker.bbrsisafe.model;

import com.ticker.bbrsisafe.rx.BbRsiSafeThread;
import com.ticker.common.model.StratThreadModel;

/**
 * The type Bb rsi safe thread model.
 */
public class BbRsiSafeThreadModel extends StratThreadModel<BbRsiSafeThread> {
    /**
     * Instantiates a new Bb rsi safe thread model.
     *
     * @param thread the thread
     */
    public BbRsiSafeThreadModel(BbRsiSafeThread thread) {
        super(thread);
    }

    /**
     * Gets dip.
     *
     * @return the dip
     */
    public double getDip() {
        return thread.getDip();
    }

    /**
     * Gets peak.
     *
     * @return the peak
     */
    public double getPeak() {
        return thread.getPeak();
    }

    /**
     * Is safe state boolean.
     *
     * @return the boolean
     */
    public boolean isSafeState() {
        return thread.getPositionQty() == 0;
    }

    /**
     * Is low target boolean.
     *
     * @return the boolean
     */
    public boolean isLowTarget() {
        return thread.isLowValue();
    }

    /**
     * Gets trade value.
     *
     * @return the trade value
     */
    public double getTradeValue() {
        return thread.getTradeValue();
    }

    /**
     * Gets trail value.
     *
     * @return the trail value
     */
    public double getTrailValue() {
        return thread.getTrailValue();
    }
}
