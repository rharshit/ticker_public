package com.ticker.brokerage.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.ticker.brokerage.common.constants.WebConstants.ZERODHA_BROKERAGE_URL;


@Service
@Slf4j
public class BrokerageService {

    private static WebDriver webDriver;

    public void getZerodhaBrokerage(String type) {

    }

    @PostConstruct
    public void init() {
        log.info("Initializing BrokerageService");
        initWebdriver();
        log.info("BrokerageService initialized");
    }

    private void initWebdriver() {
        if (webDriver == null) {
            log.info("Initializing webdriver");
            ChromeOptions options = new ChromeOptions();
            //options.setHeadless(true);
            options.addArguments("--window-size=1920,1080");
            options.addArguments("incognito");
            webDriver = new ChromeDriver(options);
            webDriver.get(ZERODHA_BROKERAGE_URL);
            log.info("Webdriver initialized");
        } else {
            log.info("Webdriver already initialized");
        }

    }
}
