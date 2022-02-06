package com.ticker.common.util.objectpool.impl;

import com.ticker.common.util.Util;
import com.ticker.common.util.objectpool.ObjectPoolData;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

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
        return Util.getWebDriver(true);
    }

    @Override
    public void destroyObject(WebDriver object) {
        try {
            object.quit();
            log.debug("Closed webdriver");

        } catch (Exception e) {
            log.error("Cannot close webdriver: " + object, e);
        }
    }
}
