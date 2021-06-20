package com.ticker.fetcher.common.rx;

import com.ticker.common.exception.TickerException;
import com.ticker.common.fetcher.repository.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.fetcher.repository.AppRepository;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.ticker.common.util.Util.WAIT_LONG;
import static com.ticker.common.util.Util.waitFor;
import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_CHART;
import static org.openqa.selenium.UnexpectedAlertBehaviour.ACCEPT;

@Getter
@Setter
@Slf4j
@Component("fetcherThread")
@Scope("prototype")
@NoArgsConstructor
public class FetcherThread extends Thread {

    @Autowired
    private AppRepository repository;

    @Autowired
    private FetcherService fetcherService;

    private TickerService tickerService;

    private ExchangeSymbolEntity entity;

    private boolean enabled = false;
    private boolean initialized = false;
    private WebDriver webDriver;
    private final Object postInitLock = new Object();
    private Set<String> fetcherApps = new HashSet<>();

    private float currentValue;
    private long updatedAt;

    public static final int RETRY_LIMIT = 10;

    /**
     * Constructor to make an object for comparison only
     *
     * @param entity
     */
    public FetcherThread(ExchangeSymbolEntity entity) {
        this.entity = entity;
    }

    public void setProperties(String... apps) {
        this.enabled = true;
        this.fetcherApps = Arrays.stream(apps).collect(Collectors.toSet());

        initializeBean();
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

    private void initializeBean() {
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
                this.webDriver.close();
            } catch (Exception e) {
                log.error("Error while closing webdriver");
            }

        }
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("incognito");
        options.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, ACCEPT);
        options.setUnhandledPromptBehaviour(ACCEPT);
        this.webDriver = new ChromeDriver(options);
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
                this.webDriver.close();
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

    public void terminateThread() {
        this.enabled = false;
        log.info("Terminating thread : " + getThreadName());
    }

    @Override
    public String toString() {
        return "FetcherThread{" +
                "exchange='" + getExchange() + '\'' +
                ", symbol='" + getSymbol() + '\'' +
                '}';
    }

    public void setTickerServiceBean(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    @Deprecated
    public void destroy() {
        tickerService.deleteTicker(this);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FetcherThread thread = (FetcherThread) o;
        return getExchange().equals(thread.getExchange()) &&
                getSymbol().equals(thread.getSymbol());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExchange(), getSymbol());
    }

    public static class Comparator implements java.util.Comparator<FetcherThread> {

        @Override
        public int compare(FetcherThread o1, FetcherThread o2) {
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (o2 == null) {
                    return -1;
                }
            }
            if (o1.getExchange().compareTo(o2.getExchange()) == 0) {
                return o1.getSymbol().compareTo(o2.getSymbol());
            } else {
                return o1.getExchange().compareTo(o2.getExchange());
            }
        }
    }
}
