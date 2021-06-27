package com.ticker.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.ticker.common.contants.TickerConstants.*;

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
            case APPLICATION_BB_RSI:
                return "http://localhost:8181/";
            case APPLICATION_MWAVE:
                return "http://localhost:8182/";
            default:
                return null;
        }
    }

    public static String generateQueryParameters(Map<String, Object> params) {
        StringBuilder url = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            url.append(url.length() == 0 ? "?" : "&")
                    .append(param.getKey())
                    .append("=")
                    .append(param.getValue() == null ? "" : param.getValue().toString());
        }
        return url.toString();
    }
}
