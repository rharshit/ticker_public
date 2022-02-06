package com.ticker.fetcher.service;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.service.TickerThreadService;
import com.ticker.fetcher.model.FetcherThreadModel;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.rx.FetcherThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.ticker.fetcher.constants.FetcherConstants.FETCHER_THREAD_COMP_NAME;

@Service
@Slf4j
public class TickerService extends TickerThreadService<FetcherThread, FetcherThreadModel> {

    @Autowired
    FetcherAppRepository repository;

    @Override
    public FetcherThreadModel createTickerThreadModel(FetcherThread thread) {
        return new FetcherThreadModel(thread);
    }

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
     * @param thread
     */
    public void deleteTicker(FetcherThread thread) {
        destroyThread(thread);
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

    public FetcherThreadModel getCurrent(String exchange, String symbol) {
        FetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return null;
        }

        return new FetcherThreadModel(thread);
    }

    public Iterable<ExchangeSymbolEntity> getAllTickers() {
        return exchangeSymbolRepository.findAll();
    }

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

    public int[] getWebdriverPoolSize() {
        return FetcherThread.getWebDrivers().poolSize();
    }
}
