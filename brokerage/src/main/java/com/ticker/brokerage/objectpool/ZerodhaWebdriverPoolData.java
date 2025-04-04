package com.ticker.brokerage.objectpool;

import com.ticker.common.exception.TickerException;
import com.ticker.common.util.objectpool.ObjectPoolData;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.remote.CapabilityType;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.ticker.brokerage.constants.WebConstants.ZERODHA_BROKERAGE_URL;
import static com.ticker.common.util.Util.WAIT_SHORT;
import static com.ticker.common.util.Util.waitFor;
import static org.openqa.selenium.UnexpectedAlertBehaviour.ACCEPT;

/**
 * The type Web driver object pool data.
 */
@Slf4j
public class ZerodhaWebdriverPoolData extends ObjectPoolData<WebDriver> {

    private Executor executor = null;

    /**
     * Instantiates a new Web driver object pool data.
     *
     * @param url the url
     */
    public ZerodhaWebdriverPoolData(String url) {
        super();
        getObject().get(url);
        log.debug("Webdriver initialized");
    }

    /**
     * Instantiates a new Web driver object pool data.
     */
    public ZerodhaWebdriverPoolData() {
        super();
        log.debug("Webdriver initialized");
    }

    private static void loadUrl(WebDriver webDriver, String url) {
        int retry = 0;
        boolean loaded = false;
        while (!loaded && retry < 5) {
            try {
                webDriver.get(url);
                loaded = true;
            } catch (Exception e) {
                retry++;
            }
        }
        if (!loaded) {
            throw new TickerException("Cannot load url: " + url);
        }
    }

    private synchronized Executor getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(5);
        }
        return executor;
    }

    @Override
    public WebDriver createObject() {
        long start = System.currentTimeMillis();
        final WebDriver[] webDriver = new WebDriver[1];
        try {
            getExecutor().execute(() -> {
                log.debug("Initializing webdriver");
                try {
                    ChromeOptions options = new ChromeOptions();
                    options.addArguments("--headless=new");
                    options.addArguments("--window-size=1920,1080");
                    options.addArguments("--disable-gpu");
                    options.addArguments("incognito");
                    options.addArguments("--remote-allow-origins=*");
                    options.setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, ACCEPT);
                    options.setUnhandledPromptBehaviour(ACCEPT);
                    if (Platform.getCurrent().is(Platform.LINUX)) {
                        System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
                        options.setBinary("/usr/bin/google-chrome-stable");
                    } else if (Platform.getCurrent().is(Platform.WINDOWS)) {
                        System.setProperty("webdriver.chrome.driver", "C:\\WebDriver\\bin\\chromedriver.exe");
                        options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
                    }
                    log.debug("Using chrome driver: {}", System.getProperty("webdriver.chrome.driver"));
                    webDriver[0] = new ChromeDriver(options);
                    log.debug("Webdriver loaded. Loading url {}", ZERODHA_BROKERAGE_URL);
                    loadUrl(webDriver[0], ZERODHA_BROKERAGE_URL);
                    log.debug("Initialized webdriver");
                } catch (Exception e) {
                    log.error("Error while creating new web driver instance", e);
                }
            });
            while (webDriver[0] == null) {
                log.trace("Waiting for webdriver to initialize");
                waitFor(WAIT_SHORT);
            }
            log.info("Webdriver created in {}ms : {}", System.currentTimeMillis() - start, webDriver[0]);
            return webDriver[0];
        } catch (Exception e) {
            log.error("Error while creating webdriver object", e);
            return null;
        }
    }

    @Override
    public void destroyObject(WebDriver object) {
        try {
            DevTools devTools = ((ChromeDriver) object).getDevTools();
            if (devTools != null) {
                ((ChromeDriver) object).getDevTools().clearListeners();
                ((ChromeDriver) object).getDevTools().disconnectSession();
            }
        } catch (Exception e) {
            log.debug("Error while closing dev session: {}", object, e);
        }
        try {
            object.quit();
            log.debug("Closed webdriver");

        } catch (Exception e) {
            log.error("Cannot close webdriver: {}", object, e);
        }
    }
}
