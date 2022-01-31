package com.ticker.fetcher.common.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.rx.TickerThread;
import com.ticker.common.util.Util;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;
import static com.ticker.common.util.Util.WAIT_LONG;
import static com.ticker.common.util.Util.waitFor;
import static com.ticker.fetcher.common.constants.FetcherConstants.FETCHER_THREAD_COMP_NAME;

@Getter
@Setter
@Slf4j
@Component(FETCHER_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class FetcherThread extends TickerThread<TickerService> {

    @Autowired
    private FetcherAppRepository repository;

    @Autowired
    private FetcherService fetcherService;

    private WebDriver webDriver;

    private final Object postInitLock = new Object();
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

    public static final int RETRY_LIMIT = 10;

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

    public String getTableName() {
        return this.entity.getFinalTableName();
    }

    protected void initializeWebDriver() {
        if (this.webDriver != null) {
            try {
                this.webDriver.quit();
            } catch (Exception e) {
                log.error("Error while closing webdriver");
            }

        }
        this.webDriver = Util.getWebDriver(true);
    }

    @Override
    public void run() {
        initialize(0, false);
        while (isEnabled()) {
            while (isEnabled() && isInitialized()) {
                waitFor(WAIT_LONG);
            }
        }

        if (this.webDriver != null) {
            try {
                this.webDriver.quit();
            } catch (Exception e) {
                log.error("Error while closing webdriver");
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
            if (refresh) {
                getWebDriver().navigate().refresh();
            } else {
                getWebDriver().get(url);
            }
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
        try {
            log.debug("Removing unwanted screens for " + getThreadName());
            if (isEnabled() && isInitialized()) {
                synchronized (postInitLock) {
                    try {
                        while (!CollectionUtils.isEmpty(
                                getWebDriver().findElements(By.cssSelector("div[data-role='toast-container']"))
                        )) {
                            getWebDriver()
                                    .findElement(By.cssSelector("div[data-role='toast-container']"))
                                    .findElement(By.tagName("article"))
                                    .findElement(By.tagName("button"))
                                    .click();
                        }
                    } catch (Exception ignored) {
                    }
                    try {
                        while (!CollectionUtils.isEmpty(
                                getWebDriver().findElements(By.cssSelector("div[data-dialog-name='gopro']"))
                        )) {
                            List<WebElement> buttons = getWebDriver()
                                    .findElement(By.cssSelector("div[data-dialog-name='gopro']"))
                                    .findElements(By.tagName("button"));
                            for (WebElement button : buttons) {
                                String classes = button.getAttribute("class");
                                if (!StringUtils.isEmpty(classes)) {
                                    if (classes.contains("close")) {
                                        button.click();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    try {
                        //indicator-properties-dialog
                        List<WebElement> buttons = getWebDriver()
                                .findElement(By.cssSelector("div[data-name='indicator-properties-dialog']"))
                                .findElements(By.cssSelector("span[data-name='close']"));
                        for (WebElement button : buttons) {
                            button.click();
                            break;
                        }
                    } catch (Exception ignore) {

                    }
                    try {
                        //style="z-index: 150;"
                        WebElement overlapManagerRoot = getWebDriver().findElement(By.id("overlap-manager-root"));
                        List<WebElement> overlaps = overlapManagerRoot.findElements(By.tagName("div"));
                        if (!CollectionUtils.isEmpty(overlaps)) {
                            for (WebElement overlap : overlaps) {
                                String text = overlap.getText();
                                if (!StringUtils.isEmpty(text) && text.contains("Take your trading to the next level")) {
                                    initialize(0, true);
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {

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

    public void refreshBrowser() {
        initialize(0, true);
    }

    public void addApp(String appName) {
        getFetcherApps().add(appName);
    }

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

    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
        this.updatedAt = System.currentTimeMillis();
    }
}
