package com.ticker.fetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.fetcher.common.rx.FetcherThread;
import com.ticker.fetcher.model.FetcherRepoModel;
import com.ticker.fetcher.repository.FetcherAppRepository;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ticker.common.util.Util.*;

@Service
@Slf4j
public class FetcherService {

    @Autowired
    private TickerService appService;

    @Autowired
    private FetcherAppRepository repository;

    @Autowired
    @Qualifier("fetcherTaskExecutor")
    private Executor fetcherTaskExecutor;

    private static final List<FetcherRepoModel> dataQueue = new ArrayList<>();

    /**
     * Set setting for the charts that are loaded
     *
     * @param fetcherThread
     * @param iteration
     * @param refresh
     */
    public void setChartSettings(FetcherThread fetcherThread, int iteration, boolean refresh) {
        int iterationMultiplier = 200;
        if (!refresh) {
            // Chart style
            waitTillLoad(fetcherThread.getWebDriver(), WAIT_SHORT + iteration * iterationMultiplier, 2);
            configureMenuByValue(fetcherThread.getWebDriver(), "menu-inner", "header-toolbar-chart-styles", "ha");

            // Chart interval
            waitTillLoad(fetcherThread.getWebDriver(), WAIT_SHORT + iteration * iterationMultiplier, 2);
            configureMenuByValue(fetcherThread.getWebDriver(), "menu-inner", "header-toolbar-intervals", "1");

        }

        // Indicators
        setIndicators(fetcherThread.getWebDriver(), WAIT_SHORT + iteration * iterationMultiplier, "bb:STD;Bollinger_Bands", "rsi:STD;RSI", "tema:STD;TEMA");

        if (refresh) {
            log.info(fetcherThread.getExchange() + ":" + fetcherThread.getSymbol() + " - Refreshed");
        } else {
            log.info(fetcherThread.getExchange() + ":" + fetcherThread.getSymbol() + " - Initialized");
        }

        fetcherThread.setInitialized(true);
    }

    /**
     * This method uses a lot extra processing and extra time
     *
     * @param webDriver
     * @param time
     * @param threshold
     */
    private void waitTillLoad(WebDriver webDriver, long time, int threshold) {
        while (webDriver.findElements(By.cssSelector("div[role='progressbar']")).size() > threshold) {
            waitFor(WAIT_SHORT);
        }
        waitFor(time);
    }

    private void setIndicators(WebDriver webDriver, long waitTime, String... indicators) {
        waitTillLoad(webDriver, waitTime, 2);
        WebElement chartStyle = webDriver.findElement(By.id("header-toolbar-indicators"));
        chartStyle.click();
        WebElement overLapManagerRoot = webDriver
                .findElement(By.id("overlap-manager-root"));
        while (CollectionUtils.isEmpty(overLapManagerRoot
                .findElements(By.cssSelector("div[data-name='indicators-dialog']")))) ;
        WebElement menuBox = overLapManagerRoot
                .findElement(By.cssSelector("div[data-name='indicators-dialog']"));

        for (String indicator : indicators) {
            waitTillLoad(webDriver, waitTime, 2);
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
            if (Platform.getCurrent().is(Platform.MAC)) {
                searchBox.sendKeys(Keys.COMMAND + "a");
            } else {
                searchBox.sendKeys(Keys.CONTROL + "a");
            }
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

    @Scheduled(fixedRate = 750)
    public void doThreadTasks() {
        List<FetcherThread> pool = appService.getCurrentTickerList();
        for (FetcherThread thread : pool) {
            try {
                fetcherTaskExecutor.execute(() -> doTask(thread));
            } catch (Exception e) {
                log.error(thread == null ? "" : (thread.getThreadName() + " : ") + e.getMessage());
            }
        }
    }

    public void doTask(FetcherThread fetcherThread) {
        log.debug("doTask start: " + fetcherThread.getThreadName());
        try {
            if (!fetcherThread.isLocked()) {
                fetcherThread.setLocked(true);
                if (fetcherThread.isInitialized() && fetcherThread.isEnabled()) {
                    try { // Get current value
                        String title = fetcherThread.getWebDriver().getTitle();
                        for (String text : title.split(" ")) {
                            if (Pattern.matches("^\\d*\\.\\d*$", text)) {
                                fetcherThread.setCurrentValue(Float.parseFloat(text));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    try { // Get OHLC Value
                        log.debug("doTask() started task: " + fetcherThread.getThreadName());
                        List<String> texts;
                        fetcherThread.getWebDriver().switchTo().window(fetcherThread.getWebDriver().getWindowHandle());
                        WebElement table = fetcherThread.getWebDriver()
                                .findElement(By.cssSelector("table[class='chart-markup-table']"));
                        Rectangle hoverBox = table.getRect();
                        Actions actions = new Actions(fetcherThread.getWebDriver());
                        actions.moveToElement(table).perform();
                        actions.moveByOffset(hoverBox.width / 2 - 1, 1 - (hoverBox.height / 2)).perform();
                        waitFor(WAIT_SHORT);
                        List<WebElement> rows = table.findElements(By.tagName("tr"));
                        texts = rows.stream().map(WebElement::getText).collect(Collectors.toList());
                        float o = 0;
                        float h = 0;
                        float l = 0;
                        float c = 0;
                        float bbA = 0;
                        float bbU = 0;
                        float bbL = 0;
                        float rsi = 0;
                        float tema = 0;
                        String ohlcbbRowText = texts.get(0);
                        if (!StringUtils.isEmpty(ohlcbbRowText)) {
                            String[] vals = ohlcbbRowText.split("\n");
                            float tmpVal = 0;
                            for (int i = 0; i < vals.length && tmpVal == 0; i++) {
                                int io = vals[i].indexOf("O");
                                int ih = vals[i].indexOf("H");
                                int il = vals[i].indexOf("L");
                                int ic = vals[i].indexOf("C");
                                if (o * h * l * c == 0 && (io < ih && ih < il && il < ic)) {
                                    String val = vals[i];

                                    o = Float.parseFloat(val.substring(io + 1, ih).trim());
                                    h = Float.parseFloat(val.substring(ih + 1, il).trim());
                                    l = Float.parseFloat(val.substring(il + 1, ic).trim());
                                    c = Float.parseFloat(val.substring(ic + 1, ic + val.substring(ic).indexOf(".") + 3).trim());

                                    fetcherThread.setO(o);
                                    fetcherThread.setH(h);
                                    fetcherThread.setL(l);
                                    fetcherThread.setC(c);

                                    log.debug("1OHLC at " + i);
                                } else if (bbL * bbA * bbU == 0 && "BB".equalsIgnoreCase(vals[i])) {
                                    bbA = Float.parseFloat(vals[i + 2]);
                                    bbU = Float.parseFloat(vals[i + 3]);
                                    bbL = Float.parseFloat(vals[i + 4]);

                                    fetcherThread.setBbA(bbA);
                                    fetcherThread.setBbU(bbU);
                                    fetcherThread.setBbL(bbL);

                                    log.debug("1BB   at " + i);

                                } else if (tema == 0 && "TEMA".equalsIgnoreCase(vals[i])) {
                                    tema = Float.parseFloat(vals[i + 2]);

                                    fetcherThread.setTema(tema);

                                    log.debug("1TEMA at " + i);
                                }
                                tmpVal = o * h * l * c * bbA * bbL * bbU * tema;
                            }
                        }

                        String rsiText = texts.get(4);
                        if (!StringUtils.isEmpty(rsiText)) {
                            String[] vals = rsiText.split("\n");
                            for (int i = 0; i < vals.length && rsi == 0; i++) {
                                if ("RSI".equalsIgnoreCase(vals[i])) {
                                    rsi = Float.parseFloat(vals[i + 2]);

                                    fetcherThread.setRsi(rsi);

                                    log.debug("1RSI  at 4");
                                }
                            }
                        }
                        float valCheck = o * h * l * c * bbL * bbA * bbU * rsi * tema;
                        if (valCheck == 0) {
                            log.error(fetcherThread.getThreadName() + " :\n" +
                                    o + "," + h + "," + l + "," + c + "\n"
                                    + bbL + "," + bbA + "," + bbU + "\n"
                                    + rsi);
                        } else {
                            log.trace(fetcherThread.getThreadName() + " :\n" +
                                    o + "," + h + "," + l + "," + c + "\n"
                                    + bbL + "," + bbA + "," + bbU + "\n"
                                    + rsi + "," + tema);
                            synchronized (dataQueue) {
                                dataQueue.add(new FetcherRepoModel(fetcherThread.getTableName(), System.currentTimeMillis(),
                                        o, h, l, c, bbU, bbA, bbL, rsi, tema));
                            }
                            log.debug("doTask() added data: " + fetcherThread.getThreadName() + ", size: " + dataQueue.size());
                        }

                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
                        log.debug(e.getClass().getName());
                        StackTraceElement[] stackTraceElements = e.getStackTrace();
                        for (StackTraceElement stackTraceElement : stackTraceElements) {
                            log.debug("\t" + stackTraceElement.toString());
                        }
                        log.info(fetcherThread.getThreadName() + " : " + e.getMessage());
                    } catch (NoSuchSessionException e) {
                        log.error("Destroying: " + fetcherThread.getThreadName());
                        fetcherThread.destroy();
                        throw e;
                    } catch (Exception e) {
                        String errorMessage = e.getMessage();
                        if (errorMessage.contains("element click intercepted")) {
                            log.info("Element click intercepted: " + fetcherThread.getThreadName());
                        } else if (errorMessage.contains("move target out of bounds")) {
                            log.info("Move target out of bounds: " + fetcherThread.getThreadName());
                        } else {
                            log.error("Exception in doTask(): " + fetcherThread.getThreadName());
                            log.error(e.getMessage());
                            throw e;
                        }
                    }
                } else {
                    log.debug("Skipping doTask()");
                }
            } else {
                log.debug(fetcherThread.getThreadName() + " : Thread locked");
            }

        } finally {
            fetcherThread.setLocked(false);
        }
        log.debug("doTask end: " + fetcherThread.getThreadName());
    }

    @Async("scheduledExecutor")
    @Scheduled(fixedRate = 850)
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
        repository.addToQueue(tempDataQueue, sNow);
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
