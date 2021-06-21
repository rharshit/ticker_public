package com.ticker.common.service;

import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolRepository;
import com.ticker.common.model.TickerThreadModel;
import com.ticker.common.rx.TickerThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Service
public abstract class TickerThreadService<T extends TickerThread, TM extends TickerThreadModel> {

    protected Set<T> threadPool;

    @Autowired
    protected ApplicationContext ctx;

    @Autowired
    protected ExchangeSymbolRepository exchangeSymbolRepository;

    public abstract void createThread(String exchange, String symbol);

    private synchronized void initializeThreadPool() {
        if (threadPool == null) {
            threadPool = new ConcurrentSkipListSet<>(new T.Comparator());
            log.info("Thread pool initialized");
        } else {
            log.info("Thread pool already initialized");
        }
    }

    protected synchronized Set<T> getThreadPool() {
        if (threadPool == null) {
            initializeThreadPool();
            log.info("Initializing thread pool");
        }
        return threadPool;
    }

    private void destroyThread(T thread) {
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info(thread.getThreadName() + " : removed thread");
    }

    public void destroyThread(String exchange, String symbol) {
        T thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info(thread.getThreadName() + " : removed thread");
    }

    protected T getThread(String exchange, String symbol) {
        Set<T> threadMap = getThreadPool();
        TickerThread compare = TickerThread.createCompareObject(new ExchangeSymbolEntity(exchange, symbol));
        return threadMap.stream().filter(thread -> thread.equals(compare)).findFirst().orElse(null);
    }

    public List<T> getCurrentTickerList() {
        return new ArrayList<>(getThreadPool());
    }

    public Map<String, List<TM>> getCurrentTickers() {
        List<T> threads = getCurrentTickerList();
        List<TM> tickers = new ArrayList<>();
        for (T thread : threads) {
            tickers.add((TM) new TickerThreadModel(thread));
        }
        Map<String, List<TM>> list = new HashMap<>();
        list.put("tickers", tickers);
        return list;
    }
}
