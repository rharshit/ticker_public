package com.ticker.bbrsi.model;

import com.ticker.bbrsi.rx.BbRsiThread;
import com.ticker.common.model.StratThreadModel;

/**
 * The type Bb rsi thread model.
 */
public class BbRsiThreadModel extends StratThreadModel<BbRsiThread> {
    /**
     * Instantiates a new Bb rsi thread model.
     *
     * @param thread the thread
     */
    public BbRsiThreadModel(BbRsiThread thread) {
        super(thread);
    }

    /**
     * Gets dip.
     *
     * @return the dip
     */
    public float getDip() {
        return thread.getDip();
    }

    /**
     * Gets peak.
     *
     * @return the peak
     */
    public float getPeak() {
        return thread.getPeak();
    }

    /**
     * Is safe state boolean.
     *
     * @return the boolean
     */
    public boolean isSafeState() {
        return thread.isSafeState();
    }

    /**
     * Is low target boolean.
     *
     * @return the boolean
     */
    public boolean isLowTarget() {
        return thread.isLowValue();
    }
}
