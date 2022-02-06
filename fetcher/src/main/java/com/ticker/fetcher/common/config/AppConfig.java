package com.ticker.fetcher.common.config;

import org.openqa.selenium.chrome.ChromeDriverService;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Level;

@Configuration
public class AppConfig {
    static {
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        System.setProperty(ChromeDriverService.CHROME_DRIVER_SILENT_OUTPUT_PROPERTY, "true");
    }
}
