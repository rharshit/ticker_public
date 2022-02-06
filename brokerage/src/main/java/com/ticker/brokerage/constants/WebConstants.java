package com.ticker.brokerage.constants;

/**
 * The type Web constants.
 */
public abstract class WebConstants {
    /**
     * The constant ZERODHA_BROKERAGE_URL.
     */
    public static final String ZERODHA_BROKERAGE_URL = "https://zerodha.com/brokerage-calculator";

    private WebConstants() {
        throw new IllegalStateException("WebConstants class");
    }
}
