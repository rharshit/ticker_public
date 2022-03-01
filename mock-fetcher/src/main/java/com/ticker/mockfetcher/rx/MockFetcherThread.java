package com.ticker.mockfetcher.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.rx.TickerThread;
import com.ticker.mockfetcher.repository.MockFetcherAppRepository;
import com.ticker.mockfetcher.service.MockFetcherService;
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

import static com.ticker.common.util.Util.WAIT_LONG;
import static com.ticker.common.util.Util.waitFor;
import static com.ticker.mockfetcher.constants.FetcherConstants.MOCK_FETCHER_THREAD_COMP_NAME;

/**
 * The type Mock fetcher thread.
 */
@Getter
@Setter
@Slf4j
@Component(MOCK_FETCHER_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class MockFetcherThread extends TickerThread<TickerService> {

    /**
     * The constant RETRY_LIMIT.
     */
    public static final int RETRY_LIMIT = 10;
    private final Object postInitLock = new Object();
    @Autowired
    private MockFetcherAppRepository repository;
    @Autowired
    private MockFetcherService fetcherService;
    private float o;
    private float h;
    private float l;
    private float c;
    private float bbU;
    private float bbA;
    private float bbL;
    private float rsi;
    private float tema;
    private float dayO;
    private float dayH;
    private float dayL;
    private float dayC;
    private float prevClose;
    private float currentValue;
    private long updatedAt;

    private long startTime;
    private long delta;

    /**
     * Sets properties.
     *
     * @param startTime the start time
     */
    public void setProperties(Long startTime) {
        this.enabled = true;
        setStartTime(startTime);

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

    /**
     * Gets table name.
     *
     * @return the table name
     */
    public String getTableName() {
        if (entity.getTableName() != null && entity.getTableName().contains(":")) {
            String[] split = entity.getTableName().split(":", 2);
            return split[0] + "_" + new SimpleDateFormat(split[1]).format(new Timestamp(getStartTime()));
        } else {
            return entity.getTableName();
        }
    }

    /**
     * Initialize web driver.
     */
    protected void initializeWebDriver() {

    }

    @Override
    public void run() {
        initialize(0, false);
        while (isEnabled()) {
            while (isEnabled() && isInitialized()) {
                waitFor(WAIT_LONG);
            }
            if (!isInitialized()) {
                initialize(0, false);
            }
        }
        log.info("Terminated thread : " + getThreadName());
    }

    /**
     * Initialize.
     *
     * @param iteration the iteration
     * @param refresh   the refresh
     */
    protected void initialize(int iteration, boolean refresh) {
        this.initialized = false;
        if (refresh) {
            log.info(getExchange() + ":" + getSymbol() + " - Refreshing");
        } else {
            log.info(getExchange() + ":" + getSymbol() + " - Initializing");
        }

        try {
            fetcherService.setDelta(this);
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

    /**
     * Refresh browser.
     */
    public void refreshBrowser() {
        initialize(0, true);
    }


    public String getThreadName() {
        return getTableName().replace(":", "_");
    }

    /**
     * Sets start time.
     *
     * @param startTime the start time
     */
    public void setStartTime(long startTime) {
        setInitialized(false);
        this.startTime = startTime;
    }

    /**
     * Sets current value.
     *
     * @param currentValue the current value
     */
    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
        this.updatedAt = System.currentTimeMillis() + getDelta();
    }
}
