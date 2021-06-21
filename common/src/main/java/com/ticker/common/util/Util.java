package com.ticker.common.util;

import lombok.extern.slf4j.Slf4j;

import static com.ticker.common.contants.TickerConstants.APPLICATION_BROKERAGE;
import static com.ticker.common.contants.TickerConstants.APPLICATION_FETCHER;

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

    public static String getApplicationUrl(String application) {
        switch (application) {
            case APPLICATION_FETCHER:
                return "http://localhost:8081/";
            case APPLICATION_BROKERAGE:
                return "http://localhost:8082/";
            default:
                return null;
        }
    }
}
