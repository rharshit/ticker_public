package com.ticker.mwave.service;

import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolRepository;
import com.ticker.mwave.model.MWaveThreadModel;
import com.ticker.mwave.rx.MWaveThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

@Slf4j
@Service
public class MWaveService {

    private static Set<MWaveThread> threadPool;
    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private ExchangeSymbolRepository exchangeSymbolRepository;

    private static synchronized Set<MWaveThread> getThreadPool() {
        if (threadPool == null) {
            initializeThreadPool();
            log.info("Initializing thread pool");
        }
        return threadPool;
    }

    private static synchronized void initializeThreadPool() {
        if (threadPool == null) {
            threadPool = new ConcurrentSkipListSet<>(new MWaveThread.Comparator());
            log.info("Thread pool initialized");
        } else {
            log.info("Thread pool already initialized");
        }
    }

    private void destroyThread(MWaveThread thread) {
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info("Removed thread: " + thread.getThreadName());
    }

    public void destroyThread(String exchange, String symbol) {
        MWaveThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info("Removed thread: " + thread.getThreadName());
    }

    private MWaveThread getThread(String exchange, String symbol) {
        Set<MWaveThread> threadMap = getThreadPool();
        MWaveThread compare = new MWaveThread(new ExchangeSymbolEntity(exchange, symbol));
        return threadMap.stream().filter(new Predicate<MWaveThread>() {
            @Override
            public boolean test(MWaveThread mWaveThread) {
                return mWaveThread.equals(compare);
            }
        }).findFirst().orElse(null);
    }

    public void createThread(String exchange, String symbol) {
        MWaveThread thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            return;
        } else {
            if (thread != null) {
                getThreadPool().remove(thread);
            }
            ExchangeSymbolEntity entity = exchangeSymbolRepository.findById(new ExchangeSymbolEntityPK(exchange, symbol)).orElse(null);
            thread = (MWaveThread) ctx.getBean("fetcherThread");
            thread.setEntity(entity);
            getThreadPool().add(thread);

            thread.setMWaveService(this);

            log.info("Added thread: " + thread.getThreadName());
            thread.start();
        }

    }

    public List<MWaveThread> getCurrentTickerList() {
        return new ArrayList<>(getThreadPool());
    }

    public Map<String, List<MWaveThreadModel>> getCurrentTickers() {
        List<MWaveThread> threads = getCurrentTickerList();
        List<MWaveThreadModel> tickers = new ArrayList<>();
        for (MWaveThread thread : threads) {
            tickers.add(new MWaveThreadModel(thread));
        }
        Map<String, List<MWaveThreadModel>> list = new HashMap<>();
        list.put("tickers", tickers);
        return list;
    }
}
