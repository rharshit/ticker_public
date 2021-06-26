package com.ticker.common.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.model.ResponseStatus;
import com.ticker.common.model.StratThreadModel;
import com.ticker.common.rx.StratThread;
import com.ticker.common.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.ticker.common.contants.TickerConstants.APPLICATION_FETCHER;

@Slf4j
@Service
public abstract class StratTickerService<T extends StratThread, TM extends StratThreadModel> extends TickerThreadService<T, TM> {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ticker.app.name}")
    private String appName;

    /**
     * @param exchange
     * @param symbol
     * @param extras   threadCompName
     */
    @Override
    public void createThread(String exchange, String symbol, String... extras) {
        T thread = getThread(exchange, symbol);
        if (thread != null && thread.isEnabled()) {
            return;
        } else {
            if (thread != null) {
                getThreadPool().remove(thread);
            }
            ExchangeSymbolEntity entity = exchangeSymbolRepository.findById(new ExchangeSymbolEntityPK(exchange, symbol)).orElse(null);
            thread = (T) ctx.getBean(extras[0]);
            thread.setEntity(entity);
            getThreadPool().add(thread);

            thread.setService(this);

            log.info(thread.getThreadName() + " : added thread");
            thread.start();
        }
    }

    @Override
    public abstract TM createTickerThreadModel(T thread);

    public void initializeThread(T t) {
        startFetching(t);
    }

    @Async("scheduledExecutor")
    @Scheduled(fixedDelay = 2000)
    public void checkFetchingForApps() {
        String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
        String getCurrentTickerUrl = baseUrl + "current/";
        for (T thread : getCurrentTickerList()) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("exchange", thread.getExchange());
                params.put("symbol", thread.getSymbol());
                Map<String, Object> ticker =
                        restTemplate.getForObject(getCurrentTickerUrl + Util.generateQueryParameters(params),
                                Map.class);
                if (ticker == null) {
                    thread.setInitialized(false);
                    thread.setFetching(false);
                    thread.setCurrentValue(0);
                } else {
                    thread.setFetching((Double) ticker.get("currentValue") != 0);
                    thread.setCurrentValue(((Double) ticker.get("currentValue")).floatValue());
                }
            } catch (Exception e) {
                thread.setFetching(false);
                thread.setCurrentValue(0);
            }
            thread.setUpdatedAt(System.currentTimeMillis());
        }
    }

    @Scheduled(fixedDelay = 750)
    public void runStrategy() {
        for (T thread : getCurrentTickerList()) {
            try {
                doAction(thread);
            } catch (Exception e) {
                log.debug(thread.getThreadName() + " : " + e.getMessage());
            }
        }
    }

    @Async("stratTaskExecutor")
    public abstract void doAction(T thread);

    @Async
    private void startFetching(T t) {
        try {
            String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
            Map<String, Object> params = new HashMap<>();
            params.put("exchange", t.getExchange());
            params.put("symbol", t.getSymbol());
            params.put("appName", appName);
            String startFetchingUrl = baseUrl + Util.generateQueryParameters(params);
            ResponseStatus response = restTemplate.postForObject(startFetchingUrl, null, ResponseStatus.class);
            if (!response.isSuccess()) {
                throw new TickerException(t.getThreadName() + " : Post request failed");
            }
        } catch (Exception e) {
            log.error(t.getThreadName() + " : Error while starting to fetch");
            t.destroy();
        }
    }


    public void stopFetching(String exchange, String symbol) {
        destroyThread(exchange, symbol);
        String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
        Map<String, Object> params = new HashMap<>();
        params.put("exchange", exchange);
        params.put("symbol", symbol);
        params.put("appName", appName);
        String deleteFetchUrl = baseUrl + Util.generateQueryParameters(params);
        restTemplate.delete(deleteFetchUrl);
    }

    public void stopFetchingAll() {
        for (T thread : getCurrentTickerList()) {
            stopFetching(thread.getExchange(), thread.getSymbol());
        }
    }

    public void refreshBrowser(String exchange, String symbol) {
        T thread = getThread(exchange, symbol);
        thread.initialize();
    }
}
