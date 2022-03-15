package com.ticker.bbrsi.model;

import com.ticker.bbrsi.rx.BbRsiThread;
import com.ticker.bbrsisafe.model.BbRsiSafeThreadModel;

/**
 * The type Bb rsi thread model.
 */
public class BbRsiThreadModel extends BbRsiSafeThreadModel<BbRsiThread> {
    /**
     * Instantiates a new Bb rsi thread model.
     *
     * @param thread the thread
     */
    public BbRsiThreadModel(BbRsiThread thread) {
        super(thread);
    }

}
