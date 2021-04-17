package com.ticker.fetcher.common.rx;

import com.ticker.fetcher.repository.AppRepository;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
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
    private FetcherService fetcherService;

    private TickerService tickerService;

    private String threadName;
    private boolean enabled;
    private String exchange;
    private String symbol;
    private int esID;
    private WebDriver webDriver;

    public static final int RETRY_LIMIT = 5;

    public void setProperties(String threadName, String exchange, String symbol, int esID) {
        this.enabled = true;
        this.threadName = threadName;
        this.exchange = exchange;
        this.symbol = symbol;
        this.esID = esID;

        initializeWebDriver();
    }

    protected void initializeWebDriver() {
        if (this.webDriver != null) {
            this.webDriver.close();
        }
        FirefoxOptions options = new FirefoxOptions();
        //options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("incognito");
        this.webDriver = new FirefoxDriver(options);
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
            fetcherService.setChartSettings(getWebDriver());
            stopWatch.stop();
            log.info(exchange + ":" + symbol + " - Initialized in " + stopWatch.getTotalTimeSeconds() + "s");
        } catch (Exception e) {
            stopWatch.stop();
            log.error("Error while initializing", e);
            log.error("Time spent: " + stopWatch.getTotalTimeSeconds() + "s");
            if (i < RETRY_LIMIT && isEnabled()) {
                initialize(i + 1);
            } else {
                tickerService.deleteTicker(this.threadName);
            }
        }
    }

    protected void doTask() {
        fetcherService.doTask(this);
    }

    @Scheduled(fixedRate = 5000)
    protected void scheduledJob() {
        fetcherService.scheduledJob(this);
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
