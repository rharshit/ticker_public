package com.ticker.fetcher.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeUtil {
    public static final long MINUTE_IN_MILLI = 60 * 1000;
    public static final long SECOND_IN_MILLI = 1000;

    public static long getMinuteTimestamp(long timestamp) {
        long minuteTimestamp = (timestamp / MINUTE_IN_MILLI) * MINUTE_IN_MILLI;
        log.trace("getMinuteTimestamp: {} -> {}", timestamp, minuteTimestamp);
        return minuteTimestamp;
    }

    public static long timeToNextMinute(long timestamp) {
        long timeToNextMinute = getMinuteTimestamp(timestamp) + (MINUTE_IN_MILLI) - timestamp;
        log.trace("timeToNextMinute: {} -> {}", timestamp, timeToNextMinute);
        return timeToNextMinute;
    }

    public static long getSecondTimestamp(long timestamp) {
        long secondTimestamp = (timestamp / SECOND_IN_MILLI) * SECOND_IN_MILLI;
        log.trace("getSecondTimestamp: {} -> {}", timestamp, secondTimestamp);
        return secondTimestamp;
    }

    public static long timeToNextSecond(long timestamp) {
        long timeToNextSecond = getSecondTimestamp(timestamp) + (SECOND_IN_MILLI) - timestamp;
        log.trace("timeToNextSecond: {} -> {}", timestamp, timeToNextSecond);
        return timeToNextSecond;
    }
}
