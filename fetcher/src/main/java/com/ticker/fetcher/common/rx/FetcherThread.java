package com.ticker.fetcher.common.rx;

import com.ticker.fetcher.repository.AppRepository;
import com.ticker.fetcher.service.TickerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.fetcher.common.constants.WebConstants.TRADING_VIEW_CHART;

@Getter
@Slf4j
@Component("fetcherThread")
@Scope("prototype")
public class FetcherThread extends Thread {

    @Autowired
    private AppRepository repository;

    @Autowired
    private TickerService service;

    private String threadName;
    private boolean enabled;
    private String exchange;
    private String symbol;
    private WebDriver webDriver;

    public static final int RETRY_LIMIT = 5;

    public void setProperties(String threadName, String exchange, String symbol) {
        this.enabled = true;
        this.threadName = threadName;
        this.exchange = exchange;
        this.symbol = symbol;

        initializeWebDriver();
    }

    protected void initializeWebDriver() {
        if (this.webDriver != null) {
            this.webDriver.close();
        }
        ChromeOptions options = new ChromeOptions();
        //options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("incognito");
        this.webDriver = new ChromeDriver(options);
    }

    @Override
    public void run() {
        initialize(0);
        while (enabled) {
            doTask();
        }
        if (this.webDriver != null) {
            this.webDriver.close();
        }
        log.info("Terminated thread : " + threadName);
    }

    protected void initialize(int i) {
        log.info(exchange + ":" + symbol + " - Initializing");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            String url = TRADING_VIEW_BASE + TRADING_VIEW_CHART + exchange + ":" + symbol;
            getWebDriver().get(url);
            service.setChartSettings(getWebDriver());
            stopWatch.stop();
            log.info(exchange + ":" + symbol + " - Initialized in " + stopWatch.getTotalTimeSeconds() + "s");
        } catch (Exception e) {
            stopWatch.stop();
            log.error("Error while initializing", e);
            log.error("Time spent: " + stopWatch.getTotalTimeSeconds() + "s");
            if (i < RETRY_LIMIT && isEnabled()) {
                initialize(i + 1);
            }
        }
    }

    protected void doTask() {
        service.doTask(this);
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
}
