package com.ticker.fetcher.service;

import com.ticker.fetcher.common.rx.FetcherThread;
import com.ticker.fetcher.repository.FetcherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FetcherService {

    @Autowired
    FetcherRepository repository;

    private static Map<String, FetcherThread> threadPool;

    private static void destroyThread(String threadName) {
        if (!threadPool.containsKey(threadName)) {
            return;
        }
        FetcherThread thread = threadPool.get(threadName);
        thread.terminateThread();
        threadPool.remove(threadName);
        log.info("Removed thread: " + threadName);
    }

    private static void createThread(String threadName) {
        if (threadPool == null) {
            initializeThreadPool();
            log.info("Initializing thread pool");
        }
        if (threadPool.containsKey(threadName)) {
            return;
        }
        FetcherThread thread = new FetcherThread(threadName) {
            //TODO: Implement actual method
            @Override
            protected void initialize() {
                log.info("Initializing thread : " + threadName);
            }

            //TODO: Implement actual method
            @Override
            protected void doTask() {
                log.info("Running thread : " + threadName);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        threadPool.put(threadName, thread);
        log.info("Added thread: " + threadName);
        thread.start();
    }

    private synchronized static void initializeThreadPool() {
        if (threadPool == null) {
            threadPool = new HashMap<>();
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
        exchange = exchange.toUpperCase();
        symbol = symbol.toUpperCase();

        int esID = repository.getExchangeSymbolId(exchange, symbol);
        log.info(String.valueOf(esID));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String date = LocalDate.now().format(formatter);

        String id = String.format("%05d", esID);
        String tickerName = date + "_" + id;

        createThread(tickerName);
    }
}
