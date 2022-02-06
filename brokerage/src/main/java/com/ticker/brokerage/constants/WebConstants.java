package com.ticker.brokerage.constants;

public abstract class WebConstants {
    public static final String ZERODHA_BROKERAGE_URL = "https://zerodha.com/brokerage-calculator";

    private WebConstants() {
        throw new IllegalStateException("WebConstants class");
    }
}
