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

/**
 * The type Ticker thread service.
 *
 * @param <T>  the type parameter
 * @param <TM> the type parameter
 */
@Slf4j
@Service
public abstract class TickerThreadService<T extends TickerThread, TM extends TickerThreadModel> extends BaseService {

    /**
     * The Thread pool.
     */
    protected Set<T> threadPool;

    /**
     * The Ctx.
     */
    @Autowired
    protected ApplicationContext ctx;

    /**
     * The Exchange symbol repository.
     */
    @Autowired
    protected ExchangeSymbolRepository exchangeSymbolRepository;

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
     * Create thread.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @param extras   the extras
     */
    public abstract void createThread(String exchange, String symbol, String... extras);

    /**
     * Override the var arg method in your service
     *
     * @param exchange       the exchange
     * @param symbol         the symbol
     * @param threadBeanName the thread bean name
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

    /**
     * Gets thread pool.
     *
     * @return the thread pool
     */
    protected synchronized Set<T> getThreadPool() {
        if (threadPool == null) {
            initializeThreadPool();
            log.info("Initializing thread pool");
        }
        return threadPool;
    }

    /**
     * Destroy thread.
     *
     * @param thread the thread
     */
    public void destroyThread(T thread) {
        if (thread == null) {
            return;
        }
        thread.terminateThread();
        getThreadPool().remove(thread);
        log.info(thread.getThreadName() + " : removed thread");
    }

    /**
     * Destroy thread.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     */
    public void destroyThread(String exchange, String symbol) {
        destroyThread(getThread(exchange, symbol));
    }

    /**
     * Gets thread.
     *
     * @param exchange the exchange
     * @param symbol   the symbol
     * @return the thread
     */
    protected T getThread(String exchange, String symbol) {
        Set<T> threadMap = getThreadPool();
        TickerThread compare = TickerThread.createCompareObject(new ExchangeSymbolEntity(exchange, symbol));
        return threadMap.stream().filter(thread -> thread.equals(compare)).findFirst().orElse(null);
    }

    /**
     * Gets current ticker list.
     *
     * @return the current ticker list
     */
    public List<T> getCurrentTickerList() {
        return new ArrayList<>(getThreadPool());
    }

    /**
     * Create ticker thread model tm.
     *
     * @param thread the thread
     * @return the tm
     */
    public abstract TM createTickerThreadModel(T thread);

    /**
     * Gets current tickers.
     *
     * @return the current tickers
     */
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
