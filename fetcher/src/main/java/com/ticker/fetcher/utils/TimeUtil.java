package com.ticker.fetcher.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeUtil {
    public static final long MINUTE_IN_MILLI = 60000;

    private TimeUtil() {
    }
    public static final long SECOND_IN_MILLI = 1000;

    public static long getMinuteTimestamp(long timestamp) {
        return getMinuteTimestamp(timestamp, 0);
    }

    public static long getMinuteTimestamp(long timestamp, int step) {
        long minuteTimestamp = ((timestamp / MINUTE_IN_MILLI) * MINUTE_IN_MILLI)
                + (step * MINUTE_IN_MILLI);
        log.trace("getMinuteTimestamp: {} -> ({}) {}", timestamp, step, minuteTimestamp);
        return minuteTimestamp;
    }

    public static long timeToNextMinute(long timestamp) {
        long timeToNextMinute = getMinuteTimestamp(timestamp, 1) - timestamp;
        log.trace("timeToNextMinute: {} -> {}", timestamp, timeToNextMinute);
        return timeToNextMinute;
    }

    public static long getSecondTimestamp(long timestamp) {
        return getSecondTimestamp(timestamp, 0);
    }

    public static long getSecondTimestamp(long timestamp, int step) {
        long secondTimestamp = (timestamp / SECOND_IN_MILLI) * SECOND_IN_MILLI
                + (step * SECOND_IN_MILLI);
        log.trace("getSecondTimestamp: {} -> ({}) {}", timestamp, step, secondTimestamp);
        return secondTimestamp;
    }

    public static long timeToNextSecond(long timestamp) {
        long timeToNextSecond = getSecondTimestamp(timestamp, 1) - timestamp;
        log.trace("timeToNextSecond: {} -> {}", timestamp, timeToNextSecond);
        return timeToNextSecond;
    }
}
