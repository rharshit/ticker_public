package com.ticker.common.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Util {
    public static final long WAIT_QUICK = 25;
    public static final long WAIT_SHORT = 250;
    public static final long WAIT_MEDIUM = 750;
    public static final long WAIT_LONG = 2000;

    public static void waitFor(long time) {
        log.debug("Waiting for " + time + "ma");
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("Error while waiting", e);
        }
        log.debug("Resume");
    }
}
