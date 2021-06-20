package com.ticker.fetcher.service;

import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolRepository;
import com.ticker.fetcher.common.rx.FetcherThread;
import com.ticker.fetcher.model.FetcherThreadModel;
import com.ticker.fetcher.repository.AppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@Service
@Slf4j
public class TickerService {

    private static Set<FetcherThread> threadPool;

    @Autowired
    AppRepository repository;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private ExchangeSymbolRepository exchangeSymbolRepository;

    private static synchronized Set<FetcherThread> getThreadPool() {
        if (threadPool == null) {
            initializeThreadPool();
            log.info("Initializing thread pool");
        }
        return threadPool;
    }

    private static synchronized void initializeThreadPool() {
        if (threadPool == null) {
            threadPool = new ConcurrentSkipListSet<>(new FetcherThread.Comparator());
            log.info("Thread pool initialized");
        } else {
            log.info("Thread pool already initialized");
        }
    }

    private FetcherThread getThread(String exchange, String symbol) {
        Set<FetcherThread> threadMap = getThreadPool();
        FetcherThread compare = new FetcherThread(new ExchangeSymbolEntity(exchange, symbol));
        return threadMap.stream().filter(compare::equals).findFirst().orElse(null);
    }

    private void destroyThread(FetcherThread thread) {
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info("Removed thread: " + thread.getThreadName());
    }

    private void destroyThread(String exchange, String symbol) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info("Removed thread: " + thread.getThreadName());
    }

    private void createThread(String exchange, String symbol, String appName) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            thread.addApp(appName);
        } else {
            if (thread != null) {
                getThreadPool().remove(thread);
            }
            ExchangeSymbolEntity entity = exchangeSymbolRepository.findById(new ExchangeSymbolEntityPK(exchange, symbol)).orElse(null);
            thread = (FetcherThread) ctx.getBean("fetcherThread");
            thread.setEntity(entity);
            getThreadPool().add(thread);
            thread.setProperties(appName);

            thread.setTickerServiceBean(this);

            log.info("Added thread: " + thread.getThreadName());
            thread.start();
        }

    }

    /**
     * Add tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange
     * @param symbol
     * @param appName
     */
    public void addTicker(String exchange, String symbol, String appName) {
        createThread(exchange, symbol, appName);
    }

    /**
     * Remove tracking for the ticker, given exchange and symbol for all apps
     *
     * @param exchange
     * @param symbol
     */
    public void deleteTicker(String exchange, String symbol) {
        destroyThread(exchange, symbol);
    }


    /**
     * Remove tracking for the ticker, given exchange and symbol for app
     *
     * @param exchange
     * @param symbol
     * @param appName
     */
    public void deleteTicker(String exchange, String symbol, String appName) {
        removeAppFromThread(exchange, symbol, appName);
    }

    private void removeAppFromThread(String exchange, String symbol, String appName) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.removeApp(appName);
    }

    /**
     * Remove tracking for the ticker, given the thread
     *
     * @param thread
     */
    public void deleteTicker(FetcherThread thread) {
        destroyThread(thread);
    }

    /**
     * Return threadPool
     *
     * @return
     */
    public Map<String, List<FetcherThreadModel>> getCurrentTickers() {
        List<FetcherThread> threads = getCurrentTickerList();
        List<FetcherThreadModel> tickers = new ArrayList<>();
        for (FetcherThread thread : threads) {
            tickers.add(new FetcherThreadModel(thread));
        }
        Map<String, List<FetcherThreadModel>> list = new HashMap<>();
        list.put("tickers", tickers);
        return list;
    }

    public List<FetcherThread> getCurrentTickerList() {
        return new ArrayList<>(getThreadPool());
    }

    public void deleteAllTickers() {
        Set<FetcherThread> threadMap = getThreadPool();
        for (FetcherThread threadName : threadMap) {
            destroyThread(threadName);
        }
    }

    @Scheduled(fixedRate = 1000)
    public void processTickers() {
        Set<FetcherThread> pool = getThreadPool();
        for (FetcherThread thread : pool) {
            if (thread.isEnabled()) {
                thread.removeUnwantedScreens();
            }
        }
    }

    public void refreshBrowser(String exchange, String symbol) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.refreshBrowser();
    }

    public float getCurrent(String exchange, String symbol) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return 0;
        }
        return thread.getCurrentValue();
    }

    public Iterable<ExchangeSymbolEntity> getAllTickers() {
        return exchangeSymbolRepository.findAll();
    }

    public ExchangeSymbolEntity addTickerToDB(String exchange, String symbol, String tickerType, Integer minQty, Integer incQty, Integer lotSize) {
        exchange = exchange.toUpperCase();
        symbol = symbol.toUpperCase();
        tickerType = tickerType.toUpperCase();
        String tableName = symbol + "_" + exchange + ":yyyy_MM_dd";
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
}
