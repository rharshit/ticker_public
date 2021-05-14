package com.ticker.brokerage.service;

import com.ticker.brokerage.common.exception.TickerException;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ticker.brokerage.common.constants.WebConstants.ZERODHA_BROKERAGE_URL;


@Service
@Slf4j
public class BrokerageService {

    private static final WebDriver webDriver;
    public static final String EQUITY = "equity";
    public static final String INTRADAY = "intraday";
    public static final String FUTURES = "futures";
    public static final String OPTIONS = "options";
    private static final Map<String, List<String>> tabs;

    static {
        log.info("Initializing webdriver");
        ChromeOptions options = new ChromeOptions();
        //options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("incognito");
        webDriver = new ChromeDriver(options);
        webDriver.get(ZERODHA_BROKERAGE_URL);
        log.info("Webdriver initialized");

        tabs = new HashMap<String, List<String>>() {{
            put(INTRADAY, Arrays.asList(INTRADAY, "id", "ide", "intraday"));
            put(EQUITY, Arrays.asList(EQUITY, "e", "de", "equities"));
            put(FUTURES, Arrays.asList(FUTURES, "f", "fof", "fnof", "future", "futures"));
            put(OPTIONS, Arrays.asList(OPTIONS, "o", "foo", "fnoo", "option", "options"));
        }};
    }

    private static String getTabType(String type) {
        StringBuilder mapping = new StringBuilder();
        for (Map.Entry<String, List<String>> tab : tabs.entrySet()) {
            mapping.append("\n");
            mapping.append(tab.getKey().toUpperCase()).append(" : ");
            for (String thisType : tab.getValue()) {
                if (thisType.equalsIgnoreCase(type)) {
                    return tab.getKey();
                }
                mapping.append(thisType).append(", ");
            }
        }
        throw new TickerException("Cannot find type for '" + type + "'. Valid options are -" + mapping.toString());
    }

    public void getZerodhaBrokerage(String type) {
        synchronized (webDriver) {
            String tabType = getTabType(type);
            log.info(tabType);
        }
    }
}
