package com.ticker.brokerage.objectpool;

import com.ticker.common.util.objectpool.ObjectPoolData;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.remote.CapabilityType;

import static org.openqa.selenium.UnexpectedAlertBehaviour.ACCEPT;

/**
 * The type Web driver object pool data.
 */
@Slf4j
public class WebDriverObjectPoolData extends ObjectPoolData<WebDriver> {

    /**
     * Instantiates a new Web driver object pool data.
     *
     * @param url the url
     */
    public WebDriverObjectPoolData(String url) {
        super();
        getObject().get(url);
        log.debug("Webdriver initialized");
    }

    /**
     * Instantiates a new Web driver object pool data.
     */
    public WebDriverObjectPoolData() {
        super();
        log.debug("Webdriver initialized");
    }

    @Override
    public WebDriver createObject() {
        WebDriver webDriver;
        log.debug("Initializing webdriver");
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("incognito");
        options.addArguments("--remote-allow-origins=*");
        options.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, ACCEPT);
        options.setUnhandledPromptBehaviour(ACCEPT);
        if (Platform.getCurrent().is(Platform.LINUX)) {
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
            options.setBinary("/usr/bin/google-chrome-stable");
        } else if (Platform.getCurrent().is(Platform.WINDOWS)) {
            System.setProperty("webdriver.chrome.driver", "C:\\WebDriver\\bin\\chromedriver.exe");
            options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        }
        log.debug("Using chrome driver: " + System.getProperty("webdriver.chrome.driver"));
        webDriver = new ChromeDriver(options);
        log.debug("Initialized webdriver");
        return webDriver;
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
            log.error("Error while closing dev session: " + object);
        }
        try {
            object.quit();
            log.debug("Closed webdriver");

        } catch (Exception e) {
            log.error("Cannot close webdriver: " + object, e);
        }
    }
}
