package com.ticker.bbrsi.constants;

/**
 * The type Bb rsi constants.
 */
public abstract class BbRsiConstants {

    /**
     * The constant BB_RSI_THREAD_COMP_NAME.
     */
    public static final String BB_RSI_THREAD_COMP_NAME = "bbRsiThread";
    /**
     * The constant BB_RSI_THREAD_STATE_STRAT_FAILED.
     */
    public static final int BB_RSI_THREAD_STATE_STRAT_FAILED = -1;
    /**
     * The constant BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER.
     */
    public static final int BB_RSI_THREAD_STATE_WAITING_FOR_TRIGGER = 1;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_TRIGGER_START.
     */
    public static final int BB_RSI_THREAD_STATE_LT_TRIGGER_START = 2;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_TRIGGER_START.
     */
    public static final int BB_RSI_THREAD_STATE_UT_TRIGGER_START = 3;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_TRIGGER_END1.
     */
    public static final int BB_RSI_THREAD_STATE_LT_TRIGGER_END1 = 4;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_TRIGGER_END2.
     */
    public static final int BB_RSI_THREAD_STATE_LT_TRIGGER_END2 = 5;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_TRIGGER_END1.
     */
    public static final int BB_RSI_THREAD_STATE_UT_TRIGGER_END1 = 6;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_TRIGGER_END2.
     */
    public static final int BB_RSI_THREAD_STATE_UT_TRIGGER_END2 = 7;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_BOUGHT.
     */
    public static final int BB_RSI_THREAD_STATE_LT_BOUGHT = 8;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_SOLD.
     */
    public static final int BB_RSI_THREAD_STATE_UT_SOLD = 9;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE.
     */
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_END_WAVE = 10;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE.
     */
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_END_WAVE = 11;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED1.
     */
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED1 = 12;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2.
     */
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED2 = 13;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED1.
     */
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED1 = 14;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2.
     */
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED2 = 15;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD.
     */
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_SOLD = 16;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT.
     */
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_BOUGHT = 17;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_WAIT_WAVE_REVENGE.
     */
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_REVENGE = 18;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_WAIT_WAVE_REVENGE.
     */
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_REVENGE = 19;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_PANIC_SELL.
     */
    public static final int BB_RSI_THREAD_STATE_LT_PANIC_SELL = 20;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_PANIC_BUY.
     */
    public static final int BB_RSI_THREAD_STATE_UT_PANIC_BUY = 21;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED3.
     */
    public static final int BB_RSI_THREAD_STATE_LT_WAIT_WAVE_ENDED3 = 22;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED3.
     */
    public static final int BB_RSI_THREAD_STATE_UT_WAIT_WAVE_ENDED3 = 23;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_GTT_FAILED.
     */
    public static final int BB_RSI_THREAD_STATE_LT_GTT_FAILED = 24;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_GTT_FAILED.
     */
    public static final int BB_RSI_THREAD_STATE_UT_GTT_FAILED = 25;
    /**
     * The constant BB_RSI_THREAD_STATE_LT_REENTER.
     */
    public static final int BB_RSI_THREAD_STATE_LT_REENTER = 26;
    /**
     * The constant BB_RSI_THREAD_STATE_UT_REENTER.
     */
    public static final int BB_RSI_THREAD_STATE_UT_REENTER = 27;

    /**
     * The constant RSI_LOWER_LIMIT.
     */
    public static final double RSI_LOWER_LIMIT = 25;
    /**
     * The constant RSI_LOWER_LIMIT_REBOUND.
     */
    public static final double RSI_LOWER_LIMIT_REBOUND = 40;
    /**
     * The constant RSI_LOWER_LIMIT_PREMATURE_DIFF.
     */
    public static final double RSI_LOWER_LIMIT_PREMATURE_DIFF = 7.5f;
    /**
     * The constant RSI_LOWER_LIMIT_PANIC_DIFF.
     */
    public static final double RSI_LOWER_LIMIT_PANIC_DIFF = -10;

    /**
     * The constant RSI_UPPER_LIMIT.
     */
    public static final double RSI_UPPER_LIMIT = 75;
    /**
     * The constant RSI_UPPER_LIMIT_REBOUND.
     */
    public static final double RSI_UPPER_LIMIT_REBOUND = 60;
    /**
     * The constant RSI_UPPER_LIMIT_PREMATURE_DIFF.
     */
    public static final double RSI_UPPER_LIMIT_PREMATURE_DIFF = -7.5f;
    /**
     * The constant RSI_UPPER_LIMIT_PANIC_DIFF.
     */
    public static final double RSI_UPPER_LIMIT_PANIC_DIFF = 10;

    /**
     * The constant PANIC_TIME_OFF.
     */
    public static final int PANIC_TIME_OFF = 40;
    /**
     * The constant PANIC_TIME_OFF_EMERGENCY_RETRIES.
     */
    public static final int PANIC_TIME_OFF_EMERGENCY_RETRIES = 5;

    /**
     * The constant GTT_RSI_UPPER_LIMIT.
     */
    public static final int GTT_RSI_UPPER_LIMIT = 87;
    /**
     * The constant GTT_RSI_LOWER_LIMIT.
     */
    public static final int GTT_RSI_LOWER_LIMIT = 13;
}
