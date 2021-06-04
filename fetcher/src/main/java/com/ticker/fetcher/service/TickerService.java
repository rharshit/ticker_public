package com.ticker.fetcher.service;

import com.ticker.fetcher.common.rx.FetcherThread;
import com.ticker.fetcher.model.FetcherThreadModel;
import com.ticker.fetcher.repository.AppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TickerService {

    private static Map<String, FetcherThread> threadPool;

    @Autowired
    AppRepository repository;

    @Autowired
    private ApplicationContext ctx;

    private synchronized static Map<String, FetcherThread> getThreadPool() {
        if (threadPool == null) {
            initializeThreadPool();
            log.info("Initializing thread pool");
        }
        return threadPool;
    }

    private synchronized static void initializeThreadPool() {
        if (threadPool == null) {
            threadPool = new ConcurrentHashMap<>();
            log.info("Thread pool initialized");
        } else {
            log.info("Thread pool already initialized");
        }
    }

    private void destroyThread(String threadName) {
        Map<String, FetcherThread> threadMap = getThreadPool();
        if (!threadMap.containsKey(threadName)) {
            return;
        }
        FetcherThread thread = threadMap.get(threadName);
        thread.terminateThread();
        threadMap.remove(threadName);
        log.info("Removed thread: " + threadName);
    }

    private void createThread(String exchange, String symbol) {
        String threadName = getThreadName(exchange, symbol);
        Map<String, FetcherThread> threadMap = getThreadPool();

        if (threadMap.containsKey(threadName)) {
            return;
        }

        int esID = getExchangeSymbolId(exchange, symbol);

        FetcherThread thread = (FetcherThread) ctx.getBean("fetcherThread");
        thread.setProperties(threadName, exchange, symbol, esID);

        thread.setTickerServiceBean(this);

        threadMap.put(threadName, thread);
        log.info("Added thread: " + threadName);
        thread.start();
    }

    /**
     * Add tracking for the ticker, given exchange and symbol
     *
     * @param exchange
     * @param symbol
     */
    public void addTicker(String exchange, String symbol) {
        createThread(exchange, symbol);
    }

    private String getThreadName(String exchange, String symbol) {
        exchange = exchange.toUpperCase();
        symbol = symbol.toUpperCase();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String date = LocalDate.now().format(formatter);

        return symbol + ":" + exchange + "_" + date;
    }

    private int getExchangeSymbolId(String exchange, String symbol) {
        return repository.getExchangeSymbolId(exchange, symbol);
    }

    /**
     * Remove tracking for the ticker, given exchange and symbol
     *
     * @param exchange
     * @param symbol
     */
    public void deleteTicker(String exchange, String symbol) {
        String threadName = getThreadName(exchange, symbol);

        destroyThread(threadName);
    }

    /**
     * Remove tracking for the ticker, given thread name
     *
     * @param threadName
     */
    public void deleteTicker(String threadName) {
        destroyThread(threadName);
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
        Map<String, List<FetcherThreadModel>> map = new HashMap<String, List<FetcherThreadModel>>() {{
            put("tickers", tickers);
        }};
        return map;
    }

    public List<FetcherThread> getCurrentTickerList() {
        Map<String, FetcherThread> threadMap = getThreadPool();
        List<FetcherThread> tickers = new ArrayList<>();
        for (Map.Entry<String, FetcherThread> entry : threadMap.entrySet()) {
            tickers.add(entry.getValue());
        }
        return tickers;
    }

    public void deleteAllTickers() {
        Map<String, FetcherThread> threadMap = getThreadPool();
        for (String threadName : threadMap.keySet()) {
            destroyThread(threadName);
        }
    }

    @Scheduled(fixedRate = 1000)
    public void processTickers() {
        Map<String, FetcherThread> pool = getThreadPool();
        for (FetcherThread thread : pool.values()) {
            if (thread.isEnabled()) {
                thread.removeUnwantedScreens();
            }
        }
    }
}
