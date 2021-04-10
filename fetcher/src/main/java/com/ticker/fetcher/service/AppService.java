package com.ticker.fetcher.service;

import com.ticker.fetcher.common.rx.FetcherThread;
import com.ticker.fetcher.repository.AppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AppService {

    @Autowired
    AppRepository repository;

    @Autowired
    private ApplicationContext ctx;

    private static Map<String, FetcherThread> threadPool;

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

        FetcherThread thread = (FetcherThread) ctx.getBean("fetcherThread");
        thread.setProperties(threadName, exchange, symbol);

        threadMap.put(threadName, thread);
        log.info("Added thread: " + threadName);
        thread.start();
    }

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

        int esID = getExchangeSymbolId(exchange, symbol);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String date = LocalDate.now().format(formatter);

        String id = String.format("%05d", esID);

        return date + "_" + id;
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
     * Return threadPool
     *
     * @return
     */
    public Map<String, String> getCurrentTickers() {
        Map<String, FetcherThread> threadMap = getThreadPool();
        Map<String, String> tickers = new HashMap<>();
        for (Map.Entry<String, FetcherThread> entry : threadMap.entrySet()) {
            tickers.put(entry.getKey(), entry.getValue().toString());
        }
        return tickers;
    }

    public void deleteAllTickers() {
        Map<String, FetcherThread> threadMap = getThreadPool();
        for (String threadName : threadMap.keySet()) {
            destroyThread(threadName);
        }
    }
}
