package com.ticker.bbrsi.constants;

public abstract class BbRsiConstants {

    public static final String BB_RSI_THREAD_COMP_NAME = "bbRsiThread";
    public static final int BB_RSI_THREAD_STATE_STRAT_FAILED = -1;
    public static final int BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER = 1;
    public static final int BB_RSI_THREAD_STATE_LT_TRIGGER_START = 2;
    public static final int BB_RSI_THREAD_STATE_UT_TRIGGER_START = 3;
    public static final int BB_RSI_THREAD_STATE_LT_TRIGGER_END1 = 4;
    public static final int BB_RSI_THREAD_STATE_LT_TRIGGER_END2 = 5;
    public static final int BB_RSI_THREAD_STATE_UT_TRIGGER_END1 = 6;
    public static final int BB_RSI_THREAD_STATE_UT_TRIGGER_END2 = 7;
    public static final int BB_RSI_THREAD_STATE_LT_BOUGHT = 8;
    public static final int BB_RSI_THREAD_STATE_UT_SOLD = 9;
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE = 10;
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE = 11;
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED1 = 12;
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2 = 13;
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED1 = 14;
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2 = 15;
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD = 16;
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT = 17;
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_REVENGE = 18;
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_REVENGE = 19;
    public static final int BB_RSI_THREAD_STATE_LT_PANIC_SELL = 20;
    public static final int BB_RSI_THREAD_STATE_UT_PANIC_BUY = 21;
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED3 = 22;
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED3 = 23;

    public static final float RSI_LOWER_LIMIT = 25;
    public static final float RSI_LOWER_LIMIT_REBOUND = 32;
    public static final float RSI_UPPER_LIMIT = 75;
    public static final float RSI_UPPER_LIMIT_REBOUND = 68;

    public static final int PANIC_TIME_OFF = 40;
    public static final int PANIC_TIME_OFF_EMERGENCY_RETRIES = 5;
}
