package com.ticker.fetcher.common.rx;

import com.ticker.fetcher.repository.AppRepository;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_CHART;
import static org.openqa.selenium.UnexpectedAlertBehaviour.ACCEPT;

@Getter
@Slf4j
@Component("fetcherThread")
@Scope("prototype")
public class FetcherThread extends Thread {

    @Autowired
    private AppRepository repository;

    @Autowired
    private FetcherService fetcherService;

    private TickerService tickerService;

    private String threadName;
    private boolean enabled = false;
    private boolean initialized = false;
    private String exchange;
    private String symbol;
    private int esID;
    private WebDriver webDriver;
    private final Object postInitLock = new Object();

    public static final int RETRY_LIMIT = 10;

    public void setProperties(String threadName, String exchange, String symbol, int esID) {
        this.enabled = true;
        this.threadName = threadName;
        this.exchange = exchange;
        this.symbol = symbol;
        this.esID = esID;

        initializeBean();
    }

    private void initializeBean() {
        initializeWebDriver();
        initializeTables();
    }

    private void initializeTables() {
        String tableName = generateTableName();
        fetcherService.createTable(tableName);
    }

    private String generateTableName() {
        return this.threadName.replace(":", "_");
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
//        options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("incognito");
        options.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, ACCEPT);
        options.setUnhandledPromptBehaviour(ACCEPT);
        this.webDriver = new ChromeDriver(options);
    }

    @Override
    public void run() {
        initialize(0);
        while (isEnabled()) {
            while (isEnabled() && isInitialized()) {
                doTask();
            }
        }

        if (this.webDriver != null) {
            try {
                this.webDriver.close();
            } catch (Exception e) {
                log.error("Error while closing webdriver");
            }
        }
        log.info("Terminated thread : " + threadName);
    }

    protected void initialize(int iteration) {
        this.initialized = false;
        log.info(exchange + ":" + symbol + " - Initializing");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            String url = TRADING_VIEW_BASE + TRADING_VIEW_CHART + exchange + ":" + symbol;
            getWebDriver().get(url);
            fetcherService.setChartSettings(getWebDriver(), iteration);
            stopWatch.stop();
            log.info(exchange + ":" + symbol + " - Initialized in " + stopWatch.getTotalTimeSeconds() + "s");
            this.initialized = true;
        } catch (Exception e) {
            stopWatch.stop();
            log.error("Error while initializing", e);
            log.error("Time spent: " + stopWatch.getTotalTimeSeconds() + "s");
            if (iteration < RETRY_LIMIT && isEnabled() && isInitialized()) {
                initialize(iteration + 1);
            } else {
                tickerService.deleteTicker(this.threadName);
            }
        }
    }

    /**
     * Remove ads and pop-ups
     */
    @Scheduled(fixedRate = 1000)
    protected void postInitializeSchedule() {
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
                                initialize(0);
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    protected void doTask() {
        fetcherService.doTask(this);
    }

    @Scheduled(fixedRate = 5000)
    protected void scheduledJob() {
        if (this.enabled) {
            fetcherService.scheduledJob(this);
        }
    }

    public void terminateThread() {
        this.enabled = false;
        log.info("Terminating thread : " + threadName);
    }

    @Override
    public String toString() {
        return "FetcherThread{" +
                "exchange='" + exchange + '\'' +
                ", symbol='" + symbol + '\'' +
                '}';
    }

    public void setTickerServiceBean(TickerService tickerService) {
        this.tickerService = tickerService;
    }
}
