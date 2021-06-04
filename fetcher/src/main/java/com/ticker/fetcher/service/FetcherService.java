package com.ticker.fetcher.service;

import com.ticker.fetcher.common.exception.TickerException;
import com.ticker.fetcher.common.rx.FetcherThread;
import com.ticker.fetcher.model.FetcherRepoModel;
import com.ticker.fetcher.repository.AppRepository;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ticker.fetcher.common.util.Util.*;

@Service
@Slf4j
public class FetcherService {

    @Autowired
    private TickerService appService;

    @Autowired
    AppRepository repository;

    private static final List<FetcherRepoModel> dataQueue = new ArrayList<>();

    private static final Pattern OLHC = Pattern.compile("^O[0-9.]*H[0-9.]*L[0-9.]*C[0-9.]*.*$");
    private static final Pattern PATTERNOS = Pattern.compile("^O");
    private static final Pattern PATTERNOE = Pattern.compile("H[0-9.]*L[0-9.]*C[0-9.]*.*$");
    private static final Pattern PATTERNHS = Pattern.compile("^O[0-9.]*H");
    private static final Pattern PATTERNHE = Pattern.compile("L[0-9.]*C[0-9.]*.*$");
    private static final Pattern PATTERNLS = Pattern.compile("^O[0-9.]*H[0-9.]*L");
    private static final Pattern PATTERNLE = Pattern.compile("C[0-9.]*.*$");
    private static final Pattern PATTERNCS = Pattern.compile("^O[0-9.]*H[0-9.]*L[0-9.]*C");
    private static final Pattern PATTERNCE = Pattern.compile("([−+][0-9.]*|00.00) \\([−+]{0,1}[0-9.]*%\\)$");

    /**
     * Set setting for the charts that are loaded
     *
     * @param fetcherThread
     * @param iteration
     * @param stopWatch
     */
    public void setChartSettings(FetcherThread fetcherThread, int iteration) {
        int iterationMultiplier = 200;
        // Chart style
        configureMenuByValue(fetcherThread.getWebDriver(), "menu-inner", "header-toolbar-chart-styles", "ha");

        // Chart interval
        configureMenuByValue(fetcherThread.getWebDriver(), "menu-inner", "header-toolbar-intervals", "1");

        // Indicators
        waitTillLoad(fetcherThread.getWebDriver(), WAIT_SHORT + iteration * iterationMultiplier, 2);
        setIndicators(fetcherThread.getWebDriver(), "bb:STD;Bollinger_Bands", "rsi:STD;RSI");

        log.info(fetcherThread.getExchange() + ":" + fetcherThread.getSymbol() + " - Initialized");
        fetcherThread.setInitialized(true);
    }

    /**
     * This method uses a lot extra processing and extra time
     * @param webDriver
     * @param time
     * @param threshold
     */
    private void waitTillLoad(WebDriver webDriver, long time, int threshold) {
        waitFor(WAIT_SHORT);
        while (webDriver.findElements(By.cssSelector("div[role='progressbar']")).size() > threshold);
        waitFor(time);
    }

    private void setIndicators(WebDriver webDriver, String... indicators) {
        WebElement chartStyle = webDriver.findElement(By.id("header-toolbar-indicators"));
        chartStyle.click();
        WebElement overLapManagerRoot = webDriver
                .findElement(By.id("overlap-manager-root"));
        while (CollectionUtils.isEmpty(overLapManagerRoot
                .findElements(By.cssSelector("div[data-name='indicators-dialog']")))) ;
        WebElement menuBox = overLapManagerRoot
                .findElement(By.cssSelector("div[data-name='indicators-dialog']"));

        for (String indicator : indicators) {
            selectIndicator(indicator, menuBox, 0);
        }
        WebElement closeBtn = overLapManagerRoot
                .findElement(By.cssSelector("span[data-name='close']"))
                .findElement(By.tagName("svg"));
        closeBtn.click();
    }

    private void selectIndicator(String indicator, WebElement menuBox, int iteration) {
        int numRetries = 5;
        log.debug("Selecting indicator " + indicator);
        try {
            String searchText = indicator.split(":")[0];
            WebElement searchBox = menuBox.findElement(By.cssSelector("input[data-role='search']"));
            waitFor(WAIT_SHORT);
            searchBox.click();
            searchBox.sendKeys(Keys.COMMAND + "a");
            searchBox.sendKeys(searchText);
            while (menuBox.findElement(By.cssSelector("div[data-role='dialog-content']")).findElements(By.cssSelector("div[data-role='list-item']")).size() <= 3)
                ;
            String indicatorId = indicator.split(":")[1];
            menuBox.findElement(By.cssSelector("div[data-id='" + indicatorId + "']")).click();

            searchBox.sendKeys(Keys.BACK_SPACE);
            while (menuBox.findElement(By.cssSelector("div[data-role='dialog-content']")).findElements(By.cssSelector("div[data-role='list-item']")).size() <= 3)
                ;
        } catch (Exception e) {
            iteration = iteration + 1;
            log.debug("Iteration " + iteration + " failed");
            if (iteration < numRetries) {
                selectIndicator(indicator, menuBox, iteration);
            } else {
                log.error(e.getMessage());
                throw new TickerException("Cannot select indicator " + indicator);
            }
        }
        log.debug("Selected indicator " + indicator);
    }

    private void configureMenuByValue(WebDriver webDriver, String dataName, String header, String value) {
        while (CollectionUtils.isEmpty(webDriver.findElements(By.id(header)))) ;
        webDriver.findElement(By.id(header)).click();
        waitFor(WAIT_MEDIUM);
        WebElement menuBox = webDriver
                .findElement(By.id("overlap-manager-root"))
                .findElement(By.cssSelector("div[data-name='" + dataName + "']"));
        WebElement valueElement = null;
        do {
            try {
                while (CollectionUtils.isEmpty(menuBox.findElements(By.cssSelector("div[data-value='" + value + "']"))))
                    ;
                valueElement = menuBox.findElement(By.cssSelector("div[data-value='" + value + "']"));
                valueElement.click();
                valueElement = null;

                webDriver.findElement(By.id(header)).click();
                while (CollectionUtils.isEmpty(menuBox.findElements(By.cssSelector("div[data-value='" + value + "']"))))
                    ;
                valueElement = menuBox.findElement(By.cssSelector("div[data-value='" + value + "']"));
            } catch (Exception e) {

                if (valueElement != null) {
                    log.error(valueElement.getCssValue("class"));
                } else {

                    if (e instanceof StaleElementReferenceException) {
                        log.debug("valueElement: " + valueElement);
                        log.debug("Error in configureMenuByValue", e);
                    } else {
                        log.error("valueElement: " + valueElement);
                        log.error("Error in configureMenuByValue", e);
                    }
                }
            }
        } while (valueElement != null && !valueElement.getCssValue("class").contains("isActive"));
        webDriver.findElement(By.id(header)).click();
    }

    @Async("fetcherTaskExecutor")
    @Scheduled(fixedDelay = 750)
    public void doThreadTasks() {
        List<FetcherThread> pool = appService.getCurrentTickerList();
        for (FetcherThread thread : pool) {
            doTask(thread);
        }
    }

    @Async("fetcherTaskExecutor")
    public void doTask(FetcherThread fetcherThread) {
        if (fetcherThread.isInitialized() && fetcherThread.isEnabled()) {
            try {
                log.debug("doTask() started task: " + fetcherThread.getThreadName());
                WebElement table = fetcherThread.getWebDriver()
                        .findElement(By.cssSelector("table[class='chart-markup-table']"));
                table.click();
                table.findElement(By.className("price-axis")).click();
                List<WebElement> rows = table.findElements(By.tagName("tr"));
                float o = 0;
                float h = 0;
                float l = 0;
                float c = 0;
                float bbA = 0;
                float bbU = 0;
                float bbL = 0;
                float rsi = 0;
                for (WebElement row : rows) {
                    String text = row.getText();
                    if (!StringUtils.isEmpty(text)) {
                        String[] vals = text.split("\n");
                        for (int i = 0; i < vals.length; i++) {
                            if (OLHC.matcher(vals[i]).matches()) {
                                String val = vals[i];

                                o = getOHCLVal(val, PATTERNOS, PATTERNOE);
                                h = getOHCLVal(val, PATTERNHS, PATTERNHE);
                                l = getOHCLVal(val, PATTERNLS, PATTERNLE);
                                c = getOHCLVal(val, PATTERNCS, PATTERNCE);

                            } else if ("BB".equalsIgnoreCase(vals[i])) {
                                bbA = Float.parseFloat(vals[i + 2]);
                                bbU = Float.parseFloat(vals[i + 3]);
                                bbL = Float.parseFloat(vals[i + 4]);
                            } else if ("RSI".equalsIgnoreCase(vals[i])) {
                                rsi = Float.parseFloat(vals[i + 2]);
                            }
                        }
                    }
                }
                float valCheck = o * h * l * c * bbL * bbA * bbU * rsi;
                if (valCheck == 0) {
                    log.error(fetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi);
                } else {
                    log.trace(fetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi);
                    synchronized (dataQueue) {
                        dataQueue.add(new FetcherRepoModel(fetcherThread.getTableName(), System.currentTimeMillis(),
                                o, h, l, c, bbU, bbA, bbL, rsi));
                    }
                    log.info("doTask() added data: " + fetcherThread.getThreadName() + ", size: " + dataQueue.size());
                }

            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                log.debug(e.getClass().getName());
                StackTraceElement[] stackTraceElements = e.getStackTrace();
                for (StackTraceElement stackTraceElement : stackTraceElements) {
                    log.debug("\t" + stackTraceElement.toString());
                }
            } catch (NoSuchSessionException e) {
                log.error("Destroying: " + fetcherThread.getThreadName());
                fetcherThread.destroy();
                throw e;
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage.contains("element click intercepted")) {
                    log.info("Element click intercepted: " + fetcherThread.getThreadName());
                } else {
                    log.error("Exception in doTask(): " + fetcherThread.getThreadName());
                    log.error(e.getMessage());
                    throw e;
                }
            }
        } else {
            log.debug("Skipping doTask()");
        }
    }

    private float getOHCLVal(String val, Pattern patternStart, Pattern patternEnd) {
        Matcher matcherStart = patternStart.matcher(val);
        Matcher matcherEnd = patternEnd.matcher(val);
        if (!matcherStart.find()) {
            log.error("No match for start pattern: " + patternStart.pattern());
            log.error(val);
        }
        if (!matcherEnd.find()) {
            log.error("No match for end pattern: " + patternEnd.pattern());
            log.error(val);
        }
        String valueString = val.substring(matcherStart.end(), matcherEnd.start());
        return Float.parseFloat(valueString);
    }

    @Async
    @Scheduled(fixedRate = 2000)
    public void scheduledJob() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String sNow = dtf.format(now);
        log.debug("Scheduled task started: " + sNow);
        List<FetcherRepoModel> tempDataQueue;
        synchronized (dataQueue) {
            tempDataQueue = new ArrayList<>(dataQueue);
            dataQueue.clear();
        }
        log.debug("Scheduled task populated: " + sNow);
        repository.pushData(tempDataQueue, sNow);
        log.debug("Scheduled task ended: " + sNow);
    }

    public void createTable(String tableName) {
        try {
            repository.addTable(tableName);
        } catch (TickerException e) {
            throw e;
        } catch (Exception e) {
            throw new TickerException("Error while crating table: " + tableName);
        }
    }
}
