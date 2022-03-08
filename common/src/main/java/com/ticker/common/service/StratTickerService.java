package com.ticker.common.service;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.exception.TickerException;
import com.ticker.common.model.ResponseStatus;
import com.ticker.common.model.StratThreadModel;
import com.ticker.common.rx.StratThread;
import com.ticker.common.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.ticker.common.contants.DateTimeConstants.*;
import static com.ticker.common.contants.TickerConstants.*;
import static com.ticker.common.util.Util.*;

/**
 * The type Strat ticker service.
 *
 * @param <T>  the type parameter
 * @param <TM> the type parameter
 */
@Slf4j
@Service
public abstract class StratTickerService<T extends StratThread, TM extends StratThreadModel> extends TickerThreadService<T, TM> {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ticker.app.name}")
    private String appName;

    @Autowired
    @Qualifier("stratTaskExecutor")
    private Executor stratTaskExecutor;

    @Autowired
    @Qualifier("fetcherExecutor")
    private Executor fetcherExecutor;

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
            long startTime = System.currentTimeMillis();
            while (!thread.isEnabled() && System.currentTimeMillis() - startTime <= 10000) {
                waitFor(WAIT_SHORT);
            }
            checkFetchingForApp(thread);
        }
    }

    @Override
    public abstract TM createTickerThreadModel(T thread);

    /**
     * Initialize thread.
     *
     * @param t the t
     */
    public void initializeThread(T t) {
        startFetching(t);
        t.setTickerType(getFavourableTickerType(t));
    }

    private void checkFetchingForApp(T thread) {
        try {
            if (thread.isEnabled()) {
                String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
                String getCurrentTickerUrl = baseUrl + "current/";
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
            }
        } catch (Exception e) {
            thread.setFetching(false);
            thread.setCurrentValue(0);
        }
        if (thread.isEnabled()) {
            waitFor(WAIT_MEDIUM);
            fetcherExecutor.execute(() -> checkFetchingForApp(thread));
        }
    }

    /**
     * Run strategy.
     */
    @Scheduled(fixedDelay = 750)
    public void runStrategy() {
        for (T thread : getCurrentTickerList()) {
            try {
                stratTaskExecutor.execute(() -> doAction(thread));
            } catch (Exception e) {
                log.debug(thread.getThreadName() + " : " + e.getMessage());
            }
        }
    }

    /**
     * Do action.
     *
     * @param thread the thread
     */
    public abstract void doAction(T thread);

    /**
     * Start fetching.
     *
     * @param thread the thread
     */
    @Async
    public void startFetching(T thread) {
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
            thread.terminateThread(false);
        }
    }


    /**
     * Stop fetching.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     */
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

    /**
     * Stop fetching all.
     */
    public void stopFetchingAll() {
        for (T thread : getCurrentTickerList()) {
            stopFetching(thread.getExchange(), thread.getSymbol());
        }
    }

    /**
     * Refresh browser.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     */
    public void refreshBrowser(String exchange, String symbol) {
        T thread = getThread(exchange, symbol);
        thread.initialize();
    }

    /**
     * Is upward trend boolean.
     *
     * @param thread the thread
     * @return the boolean
     */
    protected boolean isUpwardTrend(T thread) {
        return thread.getO() <= thread.getC();
    }

    /**
     * Is downward trend boolean.
     *
     * @param thread the thread
     * @return the boolean
     */
    protected boolean isDownwardTrend(T thread) {
        return thread.getO() >= thread.getC();
    }

    /**
     * Is eom trigger boolean.
     *
     * @return the boolean
     */
    protected boolean isEomTrigger() {
        return isEomTrigger(50);
    }

    /**
     * Is eom trigger boolean.
     *
     * @param eom the eom
     * @return the boolean
     */
    protected boolean isEomTrigger(int eom) {
        return Integer.parseInt(DATE_TIME_FORMATTER_TIME_ONLY_SECONDS.format(System.currentTimeMillis())) >= eom;
    }

    /**
     * Is same min trigger boolean.
     *
     * @param triggerTime the trigger time
     * @return the boolean
     */
    public boolean isSameMinTrigger(long triggerTime) {
        return DATE_TIME_FORMATTER_TIME_MINUTES.format(new Date(triggerTime)).equals(
                DATE_TIME_FORMATTER_TIME_MINUTES.format(new Date(System.currentTimeMillis())));
    }

    /**
     * Buy double.
     *
     * @param thread the thread
     * @param qty    the qty
     * @return the double
     */
// TODO
    protected double buy(T thread, int qty) {
        waitFor(WAIT_LONG);
        String tradeString = "Bought " + qty +
                " " + thread.getTickerType() +
                " of " + thread.getExchange() + ":" + thread.getSymbol() +
                " at " + DATE_TIME_FORMATTER_TIME_SECONDS.format(new Date(System.currentTimeMillis())) +
                " for " + thread.getCurrentValue();
        log.info(tradeString);
        String path = "logs/" + appName + "-trade" + (new SimpleDateFormat("-yyyy-MM-dd")).format(new Date(System.currentTimeMillis())) + ".log";
        Util.writeToFile(path, tradeString, true);
        thread.setPositionQty(thread.getPositionQty() + qty);
        return thread.getCurrentValue();
    }

    /**
     * Sell double.
     *
     * @param thread the thread
     * @param qty    the qty
     * @return the double
     */
// TODO
    protected double sell(T thread, int qty) {
        waitFor(WAIT_LONG);
        String tradeString = "Sold " + qty +
                " " + thread.getTickerType() +
                " of " + thread.getExchange() + ":" + thread.getSymbol() +
                " at " + DATE_TIME_FORMATTER_TIME_SECONDS.format(new Date(System.currentTimeMillis())) +
                " for " + thread.getCurrentValue();
        log.info(tradeString);
        String path = "logs/" + appName + "-trade" + (new SimpleDateFormat("-yyyy-MM-dd")).format(new Date(System.currentTimeMillis())) + ".log";
        Util.writeToFile(path, tradeString, true);
        thread.setPositionQty(thread.getPositionQty() - qty);
        return thread.getCurrentValue();
    }

    /**
     * Square off double.
     *
     * @param thread the thread
     * @return the double
     */
    protected double squareOff(T thread) {
        if (thread.getPositionQty() == 0) {
            log.warn(thread.getThreadName() + " : No positions to square-off");
        } else if (thread.getPositionQty() > 0) {
            return sell(thread, thread.getPositionQty());
        } else if (thread.getPositionQty() < 0) {
            return buy(thread, -thread.getPositionQty());
        }
        return 0;
    }

    /**
     * Gets favourable ticker type.
     *
     * @param thread the thread
     * @return the favourable ticker type
     */
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

    /**
     * Gets state value map.
     *
     * @return the state value map
     */
    public abstract Map<Integer, String> getStateValueMap();

    /**
     * Sets target threshold.
     *
     * @param thread the thread
     */
    public void setTargetThreshold(T thread) {
        long start = System.currentTimeMillis();
        boolean thresholdSet = false;
        while (!thresholdSet && System.currentTimeMillis() - start < THRESHOLD_FETCH_TIMEOUT) {
            if (thread.getCurrentValue() != 0) {
                try {
                    String url = Util.getApplicationUrl(APPLICATION_BROKERAGE) +
                            "zerodha/" +
                            thread.getTickerType() + "/" +
                            thread.getExchange();
                    Map<String, Object> params = new HashMap<>();
                    params.put("buy", thread.getCurrentValue());
                    params.put("sell", thread.getCurrentValue());
                    params.put("quantity", thread.getEntity().getMinQty());
                    Map<String, Double> response = restTemplate.getForObject(url, Map.class, params);
                    if (response.get("ptb").doubleValue() == 0) {
                        thread.setTargetThreshold(0.03f);
                    } else {
                        thread.setTargetThreshold(3 * response.get("ptb").doubleValue());
                    }
                    thresholdSet = true;
                    log.info(thread.getThreadName() + " : Target threshold set");
                    return;
                } catch (Exception e) {
                    log.info(thread.getThreadName() + " : Error while getting threshold value");
                    log.info(e.getMessage());
                }
            }
            waitFor(WAIT_MEDIUM);
        }
        thread.setTargetThreshold(0.0006f * thread.getCurrentValue());
        log.warn(thread.getThreadName() + " : Cannot fetch actual target threshold, using default");
    }

    @Override
    protected Map<String, Executor> getExecutorMap() {
        Map<String, Executor> executorMap = new HashMap<>();
        executorMap.put("StratTaskExecutor", stratTaskExecutor);
        executorMap.put("FetcherExecutor", fetcherExecutor);
        return executorMap;
    }

    @Override
    protected void sortTickers(List<TM> tickers) {
        tickers.sort((o1, o2) -> {
            if (o1.getPositionQty() == 0 && o2.getPositionQty() != 0) {
                return 1;
            } else if (o1.getPositionQty() != 0 && o2.getPositionQty() == 0) {
                return -1;
            } else if (o1.getCurrentState() == 1 && o2.getCurrentState() != 1) {
                return 1;
            } else if (o1.getCurrentState() != 1 && o2.getCurrentState() == 1) {
                return -1;
            } else if (o1.isFetching() != o2.isFetching()) {
                return o1.isFetching() ? 1 : -1;
            } else if (o1.isEnabled() != o2.isEnabled()) {
                return o1.isEnabled() ? -1 : 1;
            } else if (o1.isInitialized() != o2.isInitialized()) {
                return o1.isInitialized() ? 1 : -1;
            } else {
                return o1.getThreadName().compareTo(o2.getThreadName());
            }
        });
    }
}
