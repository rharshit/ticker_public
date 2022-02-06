package com.ticker.common.contants;

import java.text.SimpleDateFormat;

/**
 * The type Date time constants.
 */
public abstract class DateTimeConstants {
    /**
     * The constant DATE_TIME_FORMATTER_TIME_SECONDS.
     */
    public static final SimpleDateFormat DATE_TIME_FORMATTER_TIME_SECONDS = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    /**
     * The constant DATE_TIME_FORMATTER_TIME_MINUTES.
     */
    public static final SimpleDateFormat DATE_TIME_FORMATTER_TIME_MINUTES = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    /**
     * The constant DATE_TIME_FORMATTER_TIME_ONLY_SECONDS.
     */
    public static final SimpleDateFormat DATE_TIME_FORMATTER_TIME_ONLY_SECONDS = new SimpleDateFormat("ss");
}
