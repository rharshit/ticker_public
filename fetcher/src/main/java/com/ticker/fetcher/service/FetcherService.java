package com.ticker.fetcher.service;

import com.ticker.fetcher.common.exception.TickerException;
import com.ticker.fetcher.common.rx.FetcherThread;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static com.ticker.fetcher.common.util.Util.*;

@Service
@Slf4j
public class FetcherService {

    @Autowired
    private TickerService appService;

    /**
     * Set setting for the charts that are loaded
     *
     * @param webDriver
     * @param iteration
     */
    public void setChartSettings(WebDriver webDriver, int iteration) {
        int iterationMultiplier = 200;
        // Chart style
        configureMenuByValue(webDriver, "menu-inner", "header-toolbar-chart-styles", "ha");

        // Chart interval
        configureMenuByValue(webDriver, "menu-inner", "header-toolbar-intervals", "1");

        // Indicators
        waitTillLoad(webDriver, WAIT_SHORT + iteration*iterationMultiplier, 2);
        setIndicators(webDriver, "bb:STD;Bollinger_Bands", "rsi:STD;RSI");
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
        log.info("Selecting indicator " + indicator);
        try {
            String searchText = indicator.split(":")[0];
            WebElement searchBox = menuBox.findElement(By.cssSelector("input[data-role='search']"));
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
            log.error("Iteration " + iteration + " failed");
            if (iteration < numRetries) {
                selectIndicator(indicator, menuBox, iteration);
            } else {
                e.printStackTrace();
                throw new TickerException("Cannot select indicator " + indicator);
            }
        }
        log.info("Selected indicator " + indicator);
    }

    private void configureMenuByValue(WebDriver webDriver, String dataName, String header, String value) {
        while (CollectionUtils.isEmpty(webDriver.findElements(By.id(header)))) ;
        WebElement chartStyle = webDriver.findElement(By.id(header));
        chartStyle.click();
        WebElement menuBox = webDriver
                .findElement(By.id("overlap-manager-root"))
                .findElement(By.cssSelector("div[data-name='" + dataName + "']"));
        WebElement valueElement = menuBox.findElement(By.cssSelector("div[data-value='" + value + "']"));
        valueElement.click();
    }

    public void doTask(FetcherThread fetcherThread) {
        log.info("Running thread: " + fetcherThread.getThreadName());
        waitFor(WAIT_MEDIUM);
    }

    public void scheduledJob(FetcherThread fetcherThread) {
        log.info("ScheduledJob: " + fetcherThread.getThreadName());
    }
}
