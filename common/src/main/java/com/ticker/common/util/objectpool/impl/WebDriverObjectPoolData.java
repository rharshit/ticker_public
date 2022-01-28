package com.ticker.common.util.objectpool.impl;

import com.ticker.common.util.Util;
import com.ticker.common.util.objectpool.ObjectPoolData;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

@Slf4j
public class WebDriverObjectPoolData extends ObjectPoolData<WebDriver> {

    public WebDriverObjectPoolData() {
        super();
        log.info("Webdriver created");
    }

    public WebDriverObjectPoolData(String url) {
        super();
        getObject().get(url);
        log.info("Webdriver initialized");
    }

    @Override
    public WebDriver createObject() {
        return Util.getWebDriver(true);
    }

    @Override
    public void destroyObject(WebDriver object) {
        try {
            log.debug("Quitting webdriver: " + object);
            object.quit();
        } catch (Exception e) {
            log.error("Cannot quit webdriver: " + object, e);
        }
    }
}
