package com.ticker.fetcher.rx;

import com.google.common.collect.ImmutableMap;
import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.rx.TickerThread;
import com.ticker.common.util.Util;
import com.ticker.common.util.objectpool.ObjectPool;
import com.ticker.common.util.objectpool.impl.WebDriverObjectPoolData;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v97.network.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;
import static com.ticker.common.util.Util.*;
import static com.ticker.fetcher.constants.FetcherConstants.FETCHER_THREAD_COMP_NAME;

/**
 * The type Fetcher thread.
 */
@Getter
@Setter
@Slf4j
@Component(FETCHER_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class FetcherThread extends TickerThread<TickerService> {

    /**
     * The constant RETRY_LIMIT.
     */
    public static final int RETRY_LIMIT = 10;
    private static final ObjectPool<WebDriverObjectPoolData> webDrivers;

    static {
        webDrivers = new ObjectPool<WebDriverObjectPoolData>(10, 20, 45, 5000, 60000) {
            @Override
            public WebDriverObjectPoolData createObject() {
                return new WebDriverObjectPoolData();
            }
        };
    }

    private final Object postInitLock = new Object();
    @Autowired
    private FetcherAppRepository repository;
    @Autowired
    private FetcherService fetcherService;
    private WebDriver webDriver;
    private DevTools devTools;
    private Set<String> fetcherApps = new HashSet<>();
    private float o;
    private float h;
    private float l;
    private float c;
    private float bbU;
    private float bbA;
    private float bbL;
    private float rsi;
    private float tema;
    private float currentValue;
    private long updatedAt;
    private boolean taskStarted = false;
    private String studySeries;
    private String studyBB;
    private String studyRSI;
    private String studyTEMA;

    /**
     * Gets web drivers.
     *
     * @return the web drivers
     */
    public static ObjectPool<WebDriverObjectPoolData> getWebDrivers() {
        return webDrivers;
    }

    /**
     * Sets properties.
     *
     * @param apps the apps
     */
    public void setProperties(String... apps) {
        this.enabled = true;
        this.fetcherApps = Arrays.stream(apps).collect(Collectors.toSet());

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
        return this.entity.getFinalTableName();
    }

    /**
     * Initialize web driver.
     */
    protected void initializeWebDriver() {
        webDrivers.put(this.webDriver);

        while (this.webDriver == null) {
            log.debug(getThreadName() + ": Getting webdriver");
            this.webDriver = (WebDriver) webDrivers.get();
            if (this.webDriver != null) {
                break;
            }
            try {
                synchronized (webDrivers) {
                    log.trace(getThreadName() + ": Wait started");
                    webDrivers.wait(Util.WAIT_SHORT);
                    log.trace(getThreadName() + ": Wait ended");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.debug(getThreadName() + ": Got webdriver");
        devTools = ((ChromeDriver) webDriver).getDevTools();
        devTools.createSessionIfThereIsNotOne();
        devTools.send(new Command<>("Network.enable", ImmutableMap.of()));
    }

    @Override
    public void run() {
        initialize(0, false);
        while (isEnabled()) {
            while (isEnabled() && isInitialized()) {
                waitFor(WAIT_LONG);
            }
        }

        webDrivers.put(this.webDriver);
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

            String url = TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol();
            if (refresh) {
                getWebDriver().navigate().refresh();
            } else {
                getWebDriver().get(url);
            }
            devTools.clearListeners();
            devTools.addListener(Network.webSocketFrameSent(), webSocketFrameSent -> fetcherService.onSentMessage(this, webSocketFrameSent));
            fetcherService.setChartSettings(this, iteration, refresh);
            waitFor(WAIT_SHORT);
            devTools.clearListeners();
            devTools.addListener(Network.webSocketFrameReceived(), webSocketFrameReceived -> fetcherService.onReceiveMessage(this, webSocketFrameReceived));
            log.debug(getThreadName() + " :" +
                    " getStudySeries(): " + getStudySeries() +
                    " getStudyBB(): " + getStudyBB() +
                    " getStudyRSI(): " + getStudyRSI() +
                    " getStudyTEMA(): " + getStudyTEMA());
            if (ObjectUtils.isEmpty(getStudySeries()) ||
                    ObjectUtils.isEmpty(getStudyBB()) ||
                    ObjectUtils.isEmpty(getStudyRSI()) ||
                    ObjectUtils.isEmpty(getStudyTEMA())) {
                throw new TickerException(getThreadName() + " : Error initializing study name");
            }
            setInitialized(true);
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

    /**
     * Add app.
     *
     * @param appName the app name
     */
    public void addApp(String appName) {
        getFetcherApps().add(appName);
    }

    /**
     * Remove app.
     *
     * @param appName the app name
     */
    public void removeApp(String appName) {
        getFetcherApps().remove(appName);
        if (getFetcherApps().isEmpty()) {
            log.info("No apps fetching data");
            log.info("Terminating thread: " + getThreadName());
            terminateThread();
        }
    }

    public String getThreadName() {
        return getTableName().replace(":", "_");
    }

    /**
     * Gets current value.
     *
     * @return the current value
     */
    public float getCurrentValue() {
        return currentValue == 0 ? c : currentValue;
    }
}
