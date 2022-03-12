package com.ticker.fetcher.service;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.exception.TickerException;
import com.ticker.common.service.TickerThreadService;
import com.ticker.fetcher.model.FetcherThreadModel;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.rx.FetcherThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static com.ticker.fetcher.constants.FetcherConstants.FETCHER_THREAD_COMP_NAME;

/**
 * The type Ticker service.
 */
@Service
@Slf4j
public class TickerService extends TickerThreadService<FetcherThread, FetcherThreadModel> {

    /**
     * The Repository.
     */
    @Autowired
    FetcherAppRepository repository;

    @Override
    public FetcherThreadModel createTickerThreadModel(FetcherThread thread) {
        return new FetcherThreadModel(thread);
    }

    @Autowired
    @Qualifier("fetcherTaskExecutor")
    private Executor fetcherTaskExecutor;
    @Autowired
    @Qualifier("scheduledExecutor")
    private Executor scheduledExecutor;
    @Autowired
    @Qualifier("repoExecutor")
    private Executor repoExecutor;

    /**
     * Add tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange
     * @param symbol
     * @param extras   appName
     */
    @Override
    public void createThread(String exchange, String symbol, String... extras) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            thread.addApp(extras[0]);
            log.info("Added thread: " + thread.getThreadName());
        } else {
            createThread(exchange, symbol, FETCHER_THREAD_COMP_NAME);
            thread = getThread(exchange, symbol);
            if (thread == null) {
                throw new TickerException("Error while adding thread");
            }
            thread.setProperties(extras[0]);
            thread.start();
        }
    }

    /**
     * Remove tracking for the ticker, given exchange and symbol for all apps
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     */
    public void deleteTicker(String exchange, String symbol) {
        destroyThread(exchange, symbol);
    }


    /**
     * Remove tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @param appName  the app name
     */
    public void removeAppFromThread(String exchange, String symbol, String appName) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.removeApp(appName);
    }

    /**
     * Remove tracking for the ticker, given the thread
     *
     * @param thread the thread
     */
    public void deleteTicker(FetcherThread thread) {
        destroyThread(thread, false);
    }

    /**
     * Delete all tickers.
     */
    public void deleteAllTickers() {
        Set<FetcherThread> threadMap = getThreadPool();
        for (FetcherThread threadName : threadMap) {
            destroyThread(threadName, false);
        }
    }

    /**
     * Refresh browser.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     */
    public void refreshBrowser(String exchange, String symbol) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.refresh();
    }

    /**
     * Gets current.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @return the current
     */
    public FetcherThreadModel getCurrent(String exchange, String symbol) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return null;
        }

        return new FetcherThreadModel(thread);
    }

    /**
     * Gets all tickers.
     *
     * @return the all tickers
     */
    public Iterable<ExchangeSymbolEntity> getAllTickers() {
        return exchangeSymbolRepository.findAll();
    }

    /**
     * Add ticker to db exchange symbol entity.
     *
     * @param exchange   the exchange
     * @param symbol     the symbol
     * @param tickerType the ticker type
     * @param minQty     the min qty
     * @param incQty     the inc qty
     * @param lotSize    the lot size
     * @return the exchange symbol entity
     */
    public ExchangeSymbolEntity addTickerToDB(String exchange, String symbol, String tickerType, Integer minQty, Integer incQty, Integer lotSize) {
        exchange = exchange.toUpperCase();
        symbol = symbol.toUpperCase();
        tickerType = tickerType.toUpperCase();
        String tableName = (symbol + "_" + exchange + ":yyyy_MM_dd")
                .replace("!", "_excl_")
                .replaceAll("__*", "_");
        ExchangeSymbolEntity exchangeSymbolEntity;
        exchangeSymbolEntity = new ExchangeSymbolEntity(exchange, symbol, tableName,
                null, null, null, tickerType);
        if (minQty != null) {
            exchangeSymbolEntity.setMinQty(minQty);
        }
        if (incQty != null) {
            exchangeSymbolEntity.setIncQty(incQty);
        }
        if (lotSize != null) {
            exchangeSymbolEntity.setLotSize(lotSize);
        }
        return exchangeSymbolRepository.save(exchangeSymbolEntity);
    }

    /**
     * Check for unresponsive tickers.
     */
    @Async("scheduledExecutor")
    @Scheduled(fixedDelay = 2000)
    public void checkUnresponsiveTickers() {
        Set<FetcherThread> threadMap = getThreadPool();
        long now = System.currentTimeMillis();
        for (FetcherThread thread : threadMap) {
            if (thread.getLastPingAt() != 0 && now - thread.getLastPingAt() > 60000 && thread.isInitialized() && thread.isEnabled()) {
                log.info(thread.getThreadName() + " : not updated for " + (now - thread.getUpdatedAt()) + "ms");
                thread.refresh();
            }
        }
    }

    /**
     * Initialize tables on new day
     */
    @Async("repoExecutor")
    @Scheduled(cron = "5 0 0 ? * *")
    public void initializeTables() {
        log.info("Initializing tables started at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(System.currentTimeMillis())));
        Set<FetcherThread> threadMap = getThreadPool();
        long start = System.currentTimeMillis();
        for (FetcherThread thread : threadMap) {
            thread.initializeTables();
        }
        log.info("Initialized tables in " + (System.currentTimeMillis() - start) + "ms");
    }

    @Scheduled(cron = "1 15 9 ? * MON,TUE,WED,THU,FRI")
    public void refreshWebsockets() {
        log.info("Refreshing all websockets");
        Set<FetcherThread> threadMap = getThreadPool();
        for (FetcherThread thread : threadMap) {
            thread.refresh();
        }
    }

    public void updatePointValue(FetcherThread thread) {
        try {
            ExchangeSymbolEntity exchangeSymbolEntity = exchangeSymbolRepository.findById(new ExchangeSymbolEntityPK(thread.getExchange(), thread.getSymbol())).orElse(null);
            if (exchangeSymbolEntity != null) {
                int pointValue = thread.getPointValue();
                log.info(thread.getThreadName() + " : Updating point value from " + exchangeSymbolEntity.getLotSize() + " to " + pointValue);
                exchangeSymbolEntity.setMinQty(pointValue);
                exchangeSymbolEntity.setIncQty(pointValue);
                exchangeSymbolEntity.setLotSize(pointValue);

                exchangeSymbolRepository.save(exchangeSymbolEntity);
                log.info(thread.getThreadName() + " : Point value updated");
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected Map<String, Executor> getExecutorMap() {
        Map<String, Executor> executorMap = new HashMap<>();
        executorMap.put("FetcherTaskExecutor", fetcherTaskExecutor);
        executorMap.put("ScheduledExecutor", scheduledExecutor);
        executorMap.put("RepoExecutor", repoExecutor);
        return executorMap;
    }

    @Override
    protected void sortTickers(List<FetcherThreadModel> tickers) {
        tickers.sort((o1, o2) -> {
            if (o1.isEnabled() != o2.isEnabled()) {
                return o1.isEnabled() ? -1 : 1;
            } else if (o1.isInitialized() != o2.isInitialized()) {
                return o1.isInitialized() ? 1 : -1;
            } else {
                return o1.getThreadName().compareTo(o2.getThreadName());
            }
        });
    }
}
