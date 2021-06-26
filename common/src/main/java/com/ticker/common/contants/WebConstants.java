package com.ticker.common.contants;

public abstract class WebConstants {
    public static final String TRADING_VIEW_BASE = "https://in.tradingview.com/";
    public static final String TRADING_VIEW_CHART = "chart/?symbol=";

    private WebConstants() {
        throw new IllegalStateException("WebConstants class");
    }
}
