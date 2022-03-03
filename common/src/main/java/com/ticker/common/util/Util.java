package com.ticker.common.util;

import lombok.extern.slf4j.Slf4j;

import static com.ticker.common.contants.TickerConstants.*;

/**
 * The type Util.
 */
@Slf4j
public abstract class Util {
    /**
     * The constant WAIT_QUICK.
     */
    public static final long WAIT_QUICK = 25;
    /**
     * The constant WAIT_SHORT.
     */
    public static final long WAIT_SHORT = 250;
    /**
     * The constant WAIT_MEDIUM.
     */
    public static final long WAIT_MEDIUM = 750;
    /**
     * The constant WAIT_LONG.
     */
    public static final long WAIT_LONG = 2000;

    /**
     * Wait for.
     *
     * @param time the time
     */
    public static void waitFor(long time) {
        log.debug("Waiting for " + time + "ms");
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("Error while waiting", e);
        }
        log.debug("Resume");
    }

    /**
     * Gets application url.
     *
     * @param application the application
     * @return the application url
     */
    public static String getApplicationUrl(String application) {
        switch (application) {
            case APPLICATION_HOME:
                return "http://localhost:8080/";
            case APPLICATION_FETCHER:
                return "http://localhost:8081/";
            case APPLICATION_BROKERAGE:
                return "http://localhost:8082/";
            case APPLICATION_BB_RSI:
                return "http://localhost:8181/";
            case APPLICATION_BB_RSI_SAFE:
                return "http://localhost:8183/";
            case APPLICATION_MWAVE:
                return "http://localhost:8182/";
            default:
                return null;
        }
    }
}
