package com.ticker.common.service;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolRepository;
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
public abstract class TickerThreadService<T extends TickerThread, TM extends TickerThreadModel> extends BaseService {

    protected Set<T> threadPool;

    @Autowired
    protected ApplicationContext ctx;

    @Autowired
    protected ExchangeSymbolRepository exchangeSymbolRepository;

    public abstract void createThread(String exchange, String symbol, String... extras);

    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                super.run();
                log.info("Thread pool - Shutdown initiated...");
                for (T thread : getThreadPool()) {
                    destroyThread(thread);
                }
                log.info("Thread pool - Shutdown completed.");
            }
        });
    }

    /**
     * Override the var arg method in your service
     *
     * @param exchange
     * @param symbol
     * @param threadBeanName
     */
    @Deprecated
    public void createThread(String exchange, String symbol, String threadBeanName) {
        T thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            return;
        } else {
            if (thread != null) {
                getThreadPool().remove(thread);
            }
            ExchangeSymbolEntity entity = exchangeSymbolRepository.findById(new ExchangeSymbolEntityPK(exchange, symbol)).orElse(null);
            thread = (T) ctx.getBean(threadBeanName);
            thread.setEntity(entity);
            getThreadPool().add(thread);

            thread.setService(this);
        }
    }

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

    public void destroyThread(T thread) {
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info(thread.getThreadName() + " : removed thread");
    }

    public void destroyThread(String exchange, String symbol) {
        destroyThread(getThread(exchange, symbol));
    }

    protected T getThread(String exchange, String symbol) {
        Set<T> threadMap = getThreadPool();
        TickerThread compare = TickerThread.createCompareObject(new ExchangeSymbolEntity(exchange, symbol));
        return threadMap.stream().filter(thread -> thread.equals(compare)).findFirst().orElse(null);
    }

    public List<T> getCurrentTickerList() {
        return new ArrayList<>(getThreadPool());
    }

    public abstract TM createTickerThreadModel(T thread);

    public Map<String, List<TM>> getCurrentTickers() {
        List<T> threads = getCurrentTickerList();
        List<TM> tickers = new ArrayList<>();
        for (T thread : threads) {
            tickers.add(createTickerThreadModel(thread));
        }
        Map<String, List<TM>> list = new HashMap<>();
        list.put("tickers", tickers);
        return list;
    }
}
