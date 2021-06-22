package com.ticker.mwave.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntityPK;
import com.ticker.common.model.ResponseStatus;
import com.ticker.common.service.TickerThreadService;
import com.ticker.common.util.Util;
import com.ticker.mwave.model.MWaveThreadModel;
import com.ticker.mwave.rx.MWaveThread;
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
import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_STATE_STRAT_FAILED;
import static com.ticker.mwave.constants.MWaveConstants.MWAVE_THREAD_STATE_WAITING_FOR_ACTION;

@Slf4j
@Service
public class MWaveService extends TickerThreadService<MWaveThread, MWaveThreadModel> {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ticker.app.name}")
    private String appName;

    @Override
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

            thread.setService(this);

            log.info(thread.getThreadName() + " : added thread");
            thread.start();
        }

    }

    @Override
    public MWaveThreadModel createTickerThreadModel(MWaveThread thread) {
        return new MWaveThreadModel(thread);
    }

    public void initializeThread(MWaveThread mWaveThread) {
        startFetching(mWaveThread);
    }

    @Async("scheduledExecutor")
    @Scheduled(fixedDelay = 2000)
    public void checkFetchingForApps() {
        String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
        String getCurrentTickerUrl = baseUrl + "current/";
        for (MWaveThread thread : getCurrentTickerList()) {
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
        for (MWaveThread thread : getCurrentTickerList()) {
            try {
                doAction(thread);
            } catch (Exception e) {
                log.debug(thread.getThreadName() + " : " + e.getMessage());
            }
        }
    }

    @Async("stratTaskExecutor")
    private void doAction(MWaveThread thread) {
        if (!thread.isFetching()) {
            return;
        }
        switch (thread.getCurrentState()) {
            case 0:
                thread.setCurrentState(MWAVE_THREAD_STATE_WAITING_FOR_ACTION);
                break;
            case MWAVE_THREAD_STATE_WAITING_FOR_ACTION:
                checkState(thread);
                break;
            case MWAVE_THREAD_STATE_STRAT_FAILED:
                thread.destroy();
                break;
        }
    }

    // TODO: Implement method
    private void checkState(MWaveThread thread) {
        log.trace(thread.getThreadName() + " : checkState");
    }

    @Async
    private void startFetching(MWaveThread mWaveThread) {
        try {
            String baseUrl = Util.getApplicationUrl(APPLICATION_FETCHER);
            Map<String, Object> params = new HashMap<>();
            params.put("exchange", mWaveThread.getExchange());
            params.put("symbol", mWaveThread.getSymbol());
            params.put("appName", appName);
            String startFetchingUrl = baseUrl + Util.generateQueryParameters(params);
            ResponseStatus response = restTemplate.postForObject(startFetchingUrl, null, ResponseStatus.class);
            if (!response.isSuccess()) {
                throw new TickerException(mWaveThread.getThreadName() + " : Post request failed");
            }
        } catch (Exception e) {
            log.error(mWaveThread.getThreadName() + " : Error while starting to fetch");
            mWaveThread.destroy();
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
        for (MWaveThread thread : getCurrentTickerList()) {
            stopFetching(thread.getExchange(), thread.getSymbol());
        }
    }

    public void refreshBrowser(String exchange, String symbol) {
        MWaveThread thread = getThread(exchange, symbol);
        thread.initialize();
    }
}
