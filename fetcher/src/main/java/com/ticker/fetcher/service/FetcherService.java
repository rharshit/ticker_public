package com.ticker.fetcher.service;

import com.ticker.fetcher.common.rx.FetcherThread;
import com.ticker.fetcher.repository.FetcherRepository;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_CHART;
import static com.ticker.fetcher.common.util.Util.*;

@Service
@Slf4j
public class FetcherService {

    @Autowired
    FetcherRepository repository;

    private static Map<String, FetcherThread> threadPool;

    private void destroyThread(String threadName) {
        Map<String, FetcherThread> threadMap = getThreadPool();
        if (!threadMap.containsKey(threadName)) {
            return;
        }
        FetcherThread thread = threadMap.get(threadName);
        thread.terminateThread();
        threadMap.remove(threadName);
        log.info("Removed thread: " + threadName);
    }

    private void createThread(String exchange, String symbol) {
        String threadName = getThreadName(exchange, symbol);
        Map<String, FetcherThread> threadMap = getThreadPool();

        if (threadMap.containsKey(threadName)) {
            return;
        }
        FetcherThread thread = new FetcherThread(threadName, exchange, symbol) {

            @Override
            protected void initialize(int i) {
                log.info(exchange + ":" + symbol + " - Initializing");
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                try {
                    String url = TRADING_VIEW_BASE + TRADING_VIEW_CHART + exchange + ":" + symbol;
                    getWebDriver().get(url);
                    setChartSettings(getWebDriver());
                    stopWatch.stop();
                    log.info(exchange + ":" + symbol + " - Initialized in " + stopWatch.getTotalTimeSeconds() + "s");
                } catch (Exception e) {
                    stopWatch.stop();
                    log.error("Error while initializing", e);
                    log.error("Time spent: " + stopWatch.getTotalTimeSeconds() + "s");
                    if (i < RETRY_LIMIT) {
                        initialize(i + 1);
                    }

                }
            }

            //TODO: Implement actual method
            @Override
            protected void doTask() {
                log.info("Running thread : " + threadName);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        threadMap.put(threadName, thread);
        log.info("Added thread: " + threadName);
        thread.start();
    }

    private void setChartSettings(WebDriver webDriver) {
        // Chart style
        waitFor(WAIT_LONG);
        configureMenuByValue(webDriver, "menu-inner", "header-toolbar-chart-styles", "ha");

        // Chart interval
        waitFor(WAIT_LONG);
        configureMenuByValue(webDriver, "menu-inner", "header-toolbar-intervals", "1");

        // Indicators
        waitFor(WAIT_LONG);
        setIndicators(webDriver, "bb:STD;Bollinger_Bands", "rsi:STD;RSI");
    }

    private void setIndicators(WebDriver webDriver, String... indicators) {
        WebElement chartStyle = webDriver.findElement(By.id("header-toolbar-indicators"));
        chartStyle.click();
        waitFor(WAIT_MEDIUM);
        WebElement menuBox = webDriver
                .findElement(By.id("overlap-manager-root"))
                .findElement(By.cssSelector("div[data-name='indicators-dialog']"));

        while (!CollectionUtils.isEmpty(menuBox.findElements(By.cssSelector("div[role='progressbar']")))) ;

        for (String indicator : indicators) {
            String searchText = indicator.split(":")[0];
            WebElement searchBox = menuBox.findElement(By.cssSelector("input[data-role='search']"));
            searchBox.click();
            searchBox.sendKeys(searchText);
            waitFor(WAIT_SHORT);

            String indicatorId = indicator.split(":")[1];
            WebElement valueElement = menuBox.findElement(By.cssSelector("div[data-id='" + indicatorId + "']"));
            valueElement.click();

            searchBox.sendKeys(Keys.BACK_SPACE);
            waitFor(WAIT_SHORT);
        }
        WebElement closeBtn = webDriver
                .findElement(By.id("overlap-manager-root"))
                .findElement(By.cssSelector("span[data-name='close']"))
                .findElement(By.tagName("svg"));
        closeBtn.click();
    }

    private void configureMenuByValue(WebDriver webDriver, String dataName, String header, String value) {
        WebElement chartStyle = webDriver.findElement(By.id(header));
        chartStyle.click();
        WebElement menuBox = webDriver
                .findElement(By.id("overlap-manager-root"))
                .findElement(By.cssSelector("div[data-name='" + dataName + "']"));
        WebElement valueElement = menuBox.findElement(By.cssSelector("div[data-value='" + value + "']"));
        valueElement.click();
    }

    private synchronized static Map<String, FetcherThread> getThreadPool() {
        if (threadPool == null) {
            initializeThreadPool();
            log.info("Initializing thread pool");
        }
        return threadPool;
    }

    private synchronized static void initializeThreadPool() {
        if (threadPool == null) {
            threadPool = new ConcurrentHashMap<>();
            log.info("Thread pool initialized");
        } else {
            log.info("Thread pool already initialized");
        }
    }

    /**
     * Add tracking for the ticker, given exchange and symbol
     *
     * @param exchange
     * @param symbol
     */
    public void addTicker(String exchange, String symbol) {
        createThread(exchange, symbol);
    }

    private String getThreadName(String exchange, String symbol) {
        exchange = exchange.toUpperCase();
        symbol = symbol.toUpperCase();

        int esID = getExchangeSymbolId(exchange, symbol);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String date = LocalDate.now().format(formatter);

        String id = String.format("%05d", esID);

        return date + "_" + id;
    }

    private int getExchangeSymbolId(String exchange, String symbol) {
        return repository.getExchangeSymbolId(exchange, symbol);
    }

    /**
     * Remove tracking for the ticker, given exchange and symbol
     *
     * @param exchange
     * @param symbol
     */
    public void deleteTicker(String exchange, String symbol) {
        String threadName = getThreadName(exchange, symbol);

        destroyThread(threadName);
    }

    /**
     * Return threadPool
     *
     * @return
     */
    public Map<String, String> getCurrentTickers() {
        Map<String, FetcherThread> threadMap = getThreadPool();
        Map<String, String> tickers = new HashMap<>();
        for (Map.Entry<String, FetcherThread> entry : threadMap.entrySet()) {
            tickers.put(entry.getKey(), entry.getValue().toString());
        }
        return tickers;
    }

    public void deleteAllTickers() {
        Map<String, FetcherThread> threadMap = getThreadPool();
        for (String threadName : threadMap.keySet()) {
            destroyThread(threadName);
        }
    }
}
