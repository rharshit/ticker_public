package com.ticker.mockfetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.service.TickerThreadService;
import com.ticker.mockfetcher.common.rx.MockFetcherThread;
import com.ticker.mockfetcher.model.FetcherThreadModel;
import com.ticker.mockfetcher.repository.MockFetcherAppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.ticker.mockfetcher.common.constants.FetcherConstants.MOCK_FETCHER_THREAD_COMP_NAME;

@Service
@Slf4j
public class TickerService extends TickerThreadService<MockFetcherThread, FetcherThreadModel> {

    @Autowired
    MockFetcherAppRepository repository;

    @Override
    public FetcherThreadModel createTickerThreadModel(MockFetcherThread thread) {
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
        MockFetcherThread thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            thread.setStartTime(Long.parseLong(extras[0]));
            log.info("Added thread: " + thread.getThreadName());
        } else {
            createThread(exchange, symbol, MOCK_FETCHER_THREAD_COMP_NAME);
            thread = getThread(exchange, symbol);
            if (thread == null) {
                throw new TickerException("Error while adding thread");
            }
            thread.setProperties(Long.parseLong(extras[0]));
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
     * Remove tracking for the ticker, given the thread
     *
     * @param thread
     */
    public void deleteTicker(MockFetcherThread thread) {
        destroyThread(thread);
    }

    public void deleteAllTickers() {
        Set<MockFetcherThread> threadMap = getThreadPool();
        for (MockFetcherThread threadName : threadMap) {
            destroyThread(threadName);
        }
    }

    @Scheduled(fixedRate = 1000)
    public void processTickers() {
        Set<MockFetcherThread> pool = getThreadPool();
        for (MockFetcherThread thread : pool) {
            if (thread.isEnabled()) {
                thread.removeUnwantedScreens();
            }
        }
    }

    public void refreshBrowser(String exchange, String symbol) {
        MockFetcherThread thread = getThread(exchange, symbol);
        if (thread == null) {
            return;
        }
        thread.refreshBrowser();
    }

    public FetcherThreadModel getCurrent(String exchange, String symbol) {
        MockFetcherThread thread = getThread(exchange, symbol);
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
}
