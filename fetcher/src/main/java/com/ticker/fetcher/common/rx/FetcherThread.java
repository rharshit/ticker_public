package com.ticker.fetcher.common.rx;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

@Getter
@Slf4j
public abstract class FetcherThread extends Thread {
    private final String threadName;
    private boolean enabled;
    private final String exchange;
    private final String symbol;
    private WebDriver webDriver;

    public static final int RETRY_LIMIT = 5;

    protected FetcherThread(String threadName, String exchange, String symbol) {
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

    protected abstract void initialize(int i);

    protected abstract void doTask();

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
