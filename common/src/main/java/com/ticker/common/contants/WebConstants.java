package com.ticker.common.contants;

/**
 * The type Web constants.
 */
public abstract class WebConstants {
    /**
     * The constant TRADING_VIEW_BASE.
     */
    public static final String TRADING_VIEW_BASE = "https://in.tradingview.com/";
    /**
     * The constant TRADING_VIEW_CHART.
     */
    public static final String TRADING_VIEW_CHART = "chart/?symbol=";

    private WebConstants() {
        throw new IllegalStateException("WebConstants class");
    }
}
