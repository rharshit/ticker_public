package com.ticker.common.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.model.ResponseStatus;
import com.ticker.common.model.StratThreadModel;
import com.ticker.common.rx.StratThread;
import com.ticker.common.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.ticker.common.contants.DateTimeConstants.*;
import static com.ticker.common.contants.TickerConstants.*;
import static com.ticker.common.util.Util.*;

@Slf4j
@Service
public abstract class StratTickerService<T extends StratThread, TM extends StratThreadModel> extends TickerThreadService<T, TM> {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ticker.app.name}")
    private String appName;

    /**
     * @param exchange
     * @param symbol
     * @param extras   threadCompName
     */
    @Override
    public void createThread(String exchange, String symbol, String... extras) {
        T thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            return;
        } else {
            if (thread != null) {
                getThreadPool().remove(thread);
            }
            ExchangeSymbolEntity entity = exchangeSymbolRepository.findById(new ExchangeSymbolEntityPK(exchange, symbol)).orElse(null);
            thread = (T) ctx.getBean(extras[0]);
            thread.setEntity(entity);
            getThreadPool().add(thread);

            thread.setService(this);

            log.info(thread.getThreadName() + " : added thread");
            thread.start();
        }
    }

    @Override
    public abstract TM createTickerThreadModel(T thread);

    public void initializeThread(T t) {
        startFetching(t);
    }

    @Async("scheduledExecutor")
    @Scheduled(fixedDelay = 2000)
    public void checkFetchingForApps() {
        for (T thread : getCurrentTickerList()) {
            checkFetchingForApp(thread);
        }
    }

    private void checkFetchingForApp(T thread) {
        String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
        String getCurrentTickerUrl = baseUrl + "current/";
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("exchange", thread.getExchange());
            params.put("symbol", thread.getSymbol());
            Map<String, Object> ticker =
                    restTemplate.getForObject(getCurrentTickerUrl,
                            Map.class, params);
            if (ticker == null) {
                thread.setInitialized(false);
                thread.setFetching(false);
                thread.setCurrentValue(0);
            } else {
                thread.setFetchMetrics(ticker);
            }
        } catch (Exception e) {
            thread.setFetching(false);
            thread.setCurrentValue(0);
        }
        thread.setUpdatedAt(System.currentTimeMillis());
    }

    @Scheduled(fixedDelay = 750)
    public void runStrategy() {
        for (T thread : getCurrentTickerList()) {
            try {
                doAction(thread);
            } catch (Exception e) {
                log.debug(thread.getThreadName() + " : " + e.getMessage());
            }
        }
    }

    @Async("stratTaskExecutor")
    public abstract void doAction(T thread);

    @Async
    private void startFetching(T thread) {
        try {
            String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
            Map<String, Object> params = new HashMap<>();
            params.put("exchange", thread.getExchange());
            params.put("symbol", thread.getSymbol());
            params.put("appName", appName);
            ResponseStatus response = restTemplate.postForObject(baseUrl, null, ResponseStatus.class, params);
            if (!response.isSuccess()) {
                throw new TickerException(thread.getThreadName() + " : Post request failed");
            }
            return;
        } catch (Exception e) {
            log.error(thread.getThreadName() + " : Error while starting to fetch");
            log.error(thread.getThreadName(), e);
        }
        checkFetchingForApp(thread);
        if (!thread.isFetching()) {
            thread.destroy();
        }
    }


    public void stopFetching(String exchange, String symbol) {
        try {
            destroyThread(exchange, symbol);
            String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
            Map<String, Object> params = new HashMap<>();
            params.put("exchange", exchange);
            params.put("symbol", symbol);
            params.put("appName", appName);
            restTemplate.delete(baseUrl, params);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void stopFetchingAll() {
        for (T thread : getCurrentTickerList()) {
            stopFetching(thread.getExchange(), thread.getSymbol());
        }
    }

    public void refreshBrowser(String exchange, String symbol) {
        T thread = getThread(exchange, symbol);
        thread.initialize();
    }

    protected boolean isUpwardTrend(T thread) {
        return thread.getO() <= thread.getC();
    }

    protected boolean isDownwardTrend(T thread) {
        return thread.getO() >= thread.getC();
    }

    protected boolean isEomTrigger() {
        return isEomTrigger(50);
    }

    protected boolean isEomTrigger(int eom) {
        return Integer.parseInt(DATE_TIME_FORMATTER_TIME_ONLY_SECONDS.format(System.currentTimeMillis())) >= eom;
    }

    public boolean isSameMinTrigger(long triggerTime) {
        return DATE_TIME_FORMATTER_TIME_MINUTES.format(new Date(triggerTime)).equals(
                DATE_TIME_FORMATTER_TIME_MINUTES.format(new Date(System.currentTimeMillis())));
    }

    // TODO
    protected float buy(T thread, int qty) {
        waitFor(WAIT_LONG);
        log.info("Bought " + qty +
                " of " + thread.getExchange() + ":" + thread.getSymbol() +
                " at " + DATE_TIME_FORMATTER_TIME_SECONDS.format(new Date(System.currentTimeMillis())) +
                " for " + thread.getCurrentValue());
        thread.setPositionQty(thread.getPositionQty() + qty);
        return thread.getCurrentValue();
    }

    // TODO
    protected float sell(T thread, int qty) {
        waitFor(WAIT_LONG);
        log.info("Sold " + qty +
                " of " + thread.getExchange() + ":" + thread.getSymbol() +
                " at " + DATE_TIME_FORMATTER_TIME_SECONDS.format(new Date(System.currentTimeMillis())) +
                " for " + thread.getCurrentValue());
        thread.setPositionQty(thread.getPositionQty() - qty);
        return thread.getCurrentValue();
    }

    protected float squareOff(T thread) {
        if (thread.getPositionQty() == 0) {
            log.warn(thread.getThreadName() + " : No positions to square-off");
        } else if (thread.getPositionQty() > 0) {
            return sell(thread, thread.getPositionQty());
        } else if (thread.getPositionQty() < 0) {
            return buy(thread, -thread.getPositionQty());
        }
        return 0;
    }

    public String getFavourableTickerType(T thread) {
        ExchangeSymbolEntity exchangeSymbolEntity = thread.getEntity();
        String tickerTypes = exchangeSymbolEntity.getTickerType();
        if (tickerTypes == null) {
            throw new TickerException(thread.getThreadName() + " : Ticker type is empty");
        }
        if (tickerTypes.toUpperCase().contains("F")) {
            return "F";
        } else if (tickerTypes.toUpperCase().contains("I")) {
            return "I";
        } else if (tickerTypes.toUpperCase().contains("E")) {
            return "E";
        } else {
            log.warn(thread.getThreadName() + " : No ticker type found");
            return tickerTypes;
        }
    }

    public abstract Map<Integer, String> getStateValueMap();

    public void setTargetThreshold(T thread) {
        long start = System.currentTimeMillis();
        thread.setTargetThreshold(0.0006f * thread.getCurrentValue());
        while (thread.getTargetThreshold() == 0 && System.currentTimeMillis() - start < THRESHOLD_FETCH_TIMEOUT) {
            if (thread.getCurrentValue() != 0) {
                try {
                    String url = Util.getApplicationUrl(APPLICATION_BROKERAGE) +
                            "zerodha/" +
                            getFavourableTickerType(thread) + "/" +
                            thread.getExchange();
                    Map<String, Object> params = new HashMap<>();
                    params.put("buy", thread.getCurrentValue());
                    params.put("sell", thread.getCurrentValue());
                    params.put("quantity", thread.getEntity().getMinQty());
                    Map<String, Double> response = restTemplate.getForObject(url, Map.class, params);
                    thread.setTargetThreshold(3 * response.get("ptb").floatValue());
                    log.warn(thread.getThreadName() + " : Target threshold set");
                    return;
                } catch (Exception e) {
                    log.debug(thread.getThreadName() + " : Error while getting threshold value");
                    log.debug(e.getMessage());
                }
            }
            waitFor(WAIT_MEDIUM);
        }
        log.warn(thread.getThreadName() + " : Cannot fetch actual target threshold");
    }
}
