package com.ticker.common.util;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

import static com.ticker.common.contants.TickerConstants.*;
import static org.openqa.selenium.UnexpectedAlertBehaviour.ACCEPT;

/**
 * The type Util.
 */
@Slf4j
public abstract class Util {
    /**
     * The constant WAIT_QUICK.
     */
    public static final long WAIT_QUICK = 25;
    /**
     * The constant WAIT_SHORT.
     */
    public static final long WAIT_SHORT = 250;
    /**
     * The constant WAIT_MEDIUM.
     */
    public static final long WAIT_MEDIUM = 750;
    /**
     * The constant WAIT_LONG.
     */
    public static final long WAIT_LONG = 2000;

    /**
     * Wait for.
     *
     * @param time the time
     */
    public static void waitFor(long time) {
        log.debug("Waiting for " + time + "ma");
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("Error while waiting", e);
        }
        log.debug("Resume");
    }

    /**
     * Gets application url.
     *
     * @param application the application
     * @return the application url
     */
    public static String getApplicationUrl(String application) {
        switch (application) {
            case APPLICATION_HOME:
                return "http://localhost:8080/";
            case APPLICATION_FETCHER:
                return "http://localhost:8081/";
            case APPLICATION_BROKERAGE:
                return "http://localhost:8082/";
            case APPLICATION_BB_RSI:
                return "http://localhost:8181/";
            case APPLICATION_BB_RSI_SAFE:
                return "http://localhost:8183/";
            case APPLICATION_MWAVE:
                return "http://localhost:8182/";
            default:
                return null;
        }
    }

    /**
     * Gets web driver.
     *
     * @param headless the headless
     * @return the web driver
     */
    public static WebDriver getWebDriver(boolean headless) {
        WebDriver webDriver;
        log.debug("Initializing webdriver");
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(headless);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("incognito");
        options.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, ACCEPT);
        options.setUnhandledPromptBehaviour(ACCEPT);
        if (Platform.getCurrent().is(Platform.LINUX)) {
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
            options.setBinary("/usr/bin/chromium-browser");
        } else if (Platform.getCurrent().is(Platform.WINDOWS)) {
            System.setProperty("webdriver.chrome.driver", "C:\\WebDriver\\bin\\chromedriver.exe");
            options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        }
        log.debug("Using chrome driver: " + System.getProperty("webdriver.chrome.driver"));
        webDriver = new ChromeDriver(options);
        log.debug("Initialized webdriver");
        return webDriver;
    }
}
