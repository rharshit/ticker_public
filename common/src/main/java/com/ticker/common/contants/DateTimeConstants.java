package com.ticker.common.contants;

import java.text.SimpleDateFormat;

public abstract class DateTimeConstants {
    public static final SimpleDateFormat DATE_TIME_FORMATTER_TIME_SECONDS = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static final SimpleDateFormat DATE_TIME_FORMATTER_TIME_MINUTES = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    public static final SimpleDateFormat DATE_TIME_FORMATTER_TIME_ONLY_SECONDS = new SimpleDateFormat("ss");
}
