package com.ticker.fetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.service.BaseService;
import com.ticker.fetcher.model.FetcherRepoModel;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.rx.FetcherThread;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.devtools.v97.network.model.WebSocketFrameReceived;
import org.openqa.selenium.devtools.v97.network.model.WebSocketFrameSent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.ticker.common.util.Util.*;

/**
 * The type Fetcher service.
 */
@Service
@Slf4j
public class FetcherService extends BaseService {

    private static final List<FetcherRepoModel> dataQueue = new ArrayList<>();
    @Autowired
    private TickerService appService;
    @Autowired
    private FetcherAppRepository repository;
    @Autowired
    @Qualifier("fetcherTaskExecutor")
    private Executor fetcherTaskExecutor;
    @Autowired
    @Qualifier("scheduledExecutor")
    private Executor scheduledExecutor;
    @Autowired
    @Qualifier("repoExecutor")
    private Executor repoExecutor;

    /**
     * Gets executor details.
     *
     * @return the executor details
     */
    public Map<String, Map<String, Integer>> getExecutorDetails() {
        Map<String, Map<String, Integer>> details = new HashMap<>();
        details.put("fetcherTaskExecutor", getExecutorDetails(fetcherTaskExecutor));
        details.put("scheduledExecutor", getExecutorDetails(scheduledExecutor));
        details.put("repoExecutor", getExecutorDetails(repoExecutor));
        return details;
    }


    /**
     * Set setting for the charts that are loaded
     *
     * @param fetcherThread the fetcher thread
     * @param iteration     the iteration
     * @param refresh       the refresh
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

    /**
     * On message sent.
     *
     * @param thread             the thread
     * @param webSocketFrameSent the web socket frame sent
     */
    public void onSentMessage(FetcherThread thread, WebSocketFrameSent webSocketFrameSent) {
        fetcherTaskExecutor.execute(() -> {
            String[] messages = webSocketFrameSent.getResponse().getPayloadData().split("~m~\\d*~m~");
            for (String message : messages) {
                try {
                    JSONObject object = new JSONObject(message);
                    if (object.has("m") && "create_study".equals(object.getString("m")) && object.has("p")) {
                        JSONArray array = object.getJSONArray("p");
                        String series = "";
                        String name = "";
                        for (int i = 0; i < array.length(); i++) {
                            String objString = array.get(i).toString();
                            if (i == 1) {
                                name = objString;
                            }
                            if (i == 3) {
                                series = objString;
                            }
                            try {
                                JSONObject jsonObject = new JSONObject(objString);
                                if (jsonObject.has("pineId")) {
                                    String pineId = jsonObject.getString("pineId");
                                    switch (pineId) {
                                        case "STD;Bollinger_Bands":
                                            thread.setStudyBB(name);
                                            thread.setStudySeries(series);
                                            break;
                                        case "STD;RSI":
                                            thread.setStudyRSI(name);
                                            thread.setStudySeries(series);
                                            break;
                                        case "STD;TEMA":
                                            thread.setStudyTEMA(name);
                                            thread.setStudySeries(series);
                                            break;
                                    }
                                }
                            } catch (Exception ignored) {

                            }
                        }
                    }
                } catch (Exception ignored) {

                }
            }
        });
    }

    /**
     * On message received.
     *
     * @param thread                 the thread
     * @param webSocketFrameReceived the web socket frame received
     */
    public void onReceiveMessage(FetcherThread thread, WebSocketFrameReceived webSocketFrameReceived) {
        fetcherTaskExecutor.execute(() -> {
            String[] messages = webSocketFrameReceived.getResponse().getPayloadData().split("~m~\\d*~m~");
            for (String message : messages) {
                try {
                    JSONObject object = new JSONObject(message);
                    if (object.has("p")) {
                        JSONArray array = object.getJSONArray("p");
                        for (int i = 0; i < array.length(); i++) {
                            try {
                                String objString = array.get(i).toString();
                                JSONObject jsonObject = new JSONObject(objString);
                                if (jsonObject.has(thread.getStudySeries()) || jsonObject.has(thread.getStudyBB()) || jsonObject.has(thread.getStudyRSI()) || jsonObject.has(thread.getStudyTEMA())) {
                                    setVal(thread, jsonObject);
                                }
                            } catch (Exception ignored) {

                            }
                        }
                    }
                } catch (Exception ignored) {

                }
            }
        });
    }

    private void setVal(FetcherThread thread, JSONObject object) {
        log.trace(thread.getThreadName() + " : Setting value");
        for (String key : object.keySet()) {
            log.trace("Key: " + key);
            try {
                Float[] vals;
                try {
                    vals = getVals(object.getJSONObject(key).getJSONArray("st"));

                } catch (Exception e) {
                    vals = getVals(object.getJSONObject(key).getJSONArray("s"));

                }
                if (thread.getStudySeries().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting OHLC value");
                    thread.setO(vals[1]);
                    thread.setH(vals[2]);
                    thread.setL(vals[3]);
                    thread.setC(vals[4]);
                } else if (thread.getStudyBB().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting BB value");
                    thread.setBbA(vals[1]);
                    thread.setBbU(vals[2]);
                    thread.setBbL(vals[3]);
                } else if (thread.getStudyRSI().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting RSI value");
                    thread.setRsi(vals[1]);
                } else if (thread.getStudyTEMA().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting TEMA value");
                    thread.setTema(vals[1]);
                }
                thread.setUpdatedAt(System.currentTimeMillis());
                synchronized (dataQueue) {
                    dataQueue.add(new FetcherRepoModel(thread));
                }
            } catch (Exception ignored) {

            }
        }
    }


    private Float[] getVals(JSONArray arr) {
        int maxIndex = 0;
        double maxTime = 0;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject object = arr.getJSONObject(i);
                JSONArray vals = object.getJSONArray("v");
                double time = vals.getDouble(0);
                if (time > maxTime) {
                    maxIndex = i;
                    maxTime = time;
                }
            } catch (Exception ignored) {

            }
        }
        JSONObject object = arr.getJSONObject(maxIndex);
        JSONArray vals = object.getJSONArray("v");
        return vals.toList().stream().map(o -> Float.parseFloat(o.toString())).toArray(Float[]::new);
    }

    /**
     * Scheduled job.
     */
    @Async("scheduledExecutor")
    @Scheduled(fixedRate = 400)
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
        log.debug("Data size: " + tempDataQueue.size());
        repository.addToQueue(tempDataQueue, sNow);
        log.debug("Scheduled task ended: " + sNow);
        log.debug("");
    }

    /**
     * Create table.
     *
     * @param tableName the table name
     */
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
