package com.ticker.mockfetcher.common.rx;

import com.ticker.common.exception.TickerException;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.rx.TickerThread;
import com.ticker.mockfetcher.repository.FetcherAppRepository;
import com.ticker.mockfetcher.service.FetcherService;
import com.ticker.mockfetcher.service.TickerService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;
import static com.ticker.common.util.Util.WAIT_LONG;
import static com.ticker.common.util.Util.waitFor;
import static com.ticker.mockfetcher.common.constants.FetcherConstants.MOCK_FETCHER_THREAD_COMP_NAME;

@Getter
@Setter
@Slf4j
@Component(MOCK_FETCHER_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class MockFetcherThread extends TickerThread<TickerService> {

    public static final int RETRY_LIMIT = 10;
    private final Object postInitLock = new Object();
    @Autowired
    private FetcherAppRepository repository;
    @Autowired
    private FetcherService fetcherService;
    private float o;
    private float h;
    private float l;
    private float c;
    private float bbU;
    private float bbA;
    private float bbL;
    private float rsi;
    private float currentValue;
    private long updatedAt;

    private long startTime;

    public void setProperties(Long startTime) {
        this.enabled = true;
        this.startTime = startTime;

        initialize();
    }

    public void setEntity(ExchangeSymbolEntity entity) {
        if (entity == null) {
            throw new TickerException("No entity found for the given exchange and symbol");
        }
        this.entity = entity;
    }

    public String getExchange() {
        return entity.getExchangeId();
    }

    public String getSymbol() {
        return entity.getSymbolId();
    }

    @Override
    protected void initialize() {
        initializeWebDriver();
        initializeTables();
    }

    private void initializeTables() {
        String tableName = getTableName();
        fetcherService.createTable(tableName);
    }

    public String getTableName() {
        if (entity.getTableName() != null && entity.getTableName().contains(":")) {
            String[] split = entity.getTableName().split(":", 2);
            return split[0] + "_" + new SimpleDateFormat(split[1]).format(new Timestamp(getStartTime()));
        } else {
            return entity.getTableName();
        }
    }

    protected void initializeWebDriver() {

    }

    @Override
    public void run() {
        initialize(0, false);
        while (isEnabled()) {
            while (isEnabled() && isInitialized()) {
                waitFor(WAIT_LONG);
            }
        }
        log.info("Terminated thread : " + getThreadName());
    }

    protected void initialize(int iteration, boolean refresh) {
        this.initialized = false;
        if (refresh) {
            log.info(getExchange() + ":" + getSymbol() + " - Refreshing");
        } else {
            log.info(getExchange() + ":" + getSymbol() + " - Initializing");
        }

        try {
            String url = TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol();
            fetcherService.setChartSettings(this, iteration, refresh);
        } catch (Exception e) {
            if (refresh) {
                log.warn("Error while refreshing " + getThreadName());
            } else {
                log.warn("Error while initializing " + getThreadName());
            }

            if (iteration < RETRY_LIMIT && isEnabled()) {
                initialize(iteration + 1, refresh);
            } else {
                if (refresh) {
                    log.error("Error while refreshing " + getThreadName(), e);
                } else {
                    log.error("Error while initializing " + getThreadName(), e);
                }
                log.error("Destorying " + getThreadName());
                destroy();
            }
        }
    }

    /**
     * Remove ads and pop-ups
     */
    public void removeUnwantedScreens() {

    }

    @Override
    public String toString() {
        return "FetcherThread{" +
                "exchange='" + getExchange() + '\'' +
                ", symbol='" + getSymbol() + '\'' +
                '}';
    }

    @Deprecated
    public void destroy() {
        service.deleteTicker(this);
    }

    public void refreshBrowser() {
        initialize(0, true);
    }


    public String getThreadName() {
        return getTableName().replace(":", "_");
    }

    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
        this.updatedAt = System.currentTimeMillis();
    }
}
