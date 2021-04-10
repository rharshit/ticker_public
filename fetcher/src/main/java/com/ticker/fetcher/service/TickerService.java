package com.ticker.fetcher.service;

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
public class TickerService {

    @Autowired
    private AppService appService;

    /**
     * Set setting for the charts that are loaded
     *
     * @param webDriver
     */
    public void setChartSettings(WebDriver webDriver) {
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

    public void doTask(FetcherThread fetcherThread) {
        log.info("Running thread: " + fetcherThread.getThreadName());
        waitFor(WAIT_MEDIUM);
    }

    public void scheduledJob(FetcherThread fetcherThread) {
        log.info("ScheduledJob: " + fetcherThread.getThreadName());
    }
}
