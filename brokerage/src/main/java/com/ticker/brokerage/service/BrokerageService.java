package com.ticker.brokerage.service;

import com.ticker.brokerage.common.exception.TickerException;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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

    private static WebDriver webDriver;
    private static final String EQUITY = "equity";
    private static final String INTRADAY = "intraday";
    private static final String FUTURES = "futures";
    private static final String OPTIONS = "options";
    private static final Map<String, List<String>> tabs;
    public static final int numTries = 3;

    static {
        initWebdriver();

        tabs = new HashMap<String, List<String>>() {{
            put(INTRADAY, Arrays.asList(INTRADAY, "id", "ide", "intraday"));
            put(EQUITY, Arrays.asList(EQUITY, "e", "de", "equities"));
            put(FUTURES, Arrays.asList(FUTURES, "f", "fof", "fnof", "future", "futures"));
            put(OPTIONS, Arrays.asList(OPTIONS, "o", "foo", "fnoo", "option", "options"));
        }};
    }

    private static void initWebdriver() {
        log.info("Initializing webdriver");
        ChromeOptions options = new ChromeOptions();
        //options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("incognito");
        webDriver = new ChromeDriver(options);
        webDriver.get(ZERODHA_BROKERAGE_URL);
        log.info("Webdriver initialized");
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

    public Map<String, Double> getZerodhaBrokerage(String type, int numTry) {
        String tabType = getTabType(type);
        log.info(tabType);
        String divId = null;
        switch (tabType) {
            case INTRADAY:
                divId = "intraday-equity-calc";
                break;
            case EQUITY:
                divId = "delivery-equity-calc";
                break;
            case FUTURES:
                divId = "futures-equity-calc";
                break;
            case OPTIONS:
                divId = "options-equity-calc";
                break;
        }
        Map<String, Double> data = new HashMap<>();
        try {
            synchronized (webDriver) {
                WebElement tabDiv = webDriver.findElement(By.id(divId));
                List<WebElement> divs = tabDiv.findElements(By.className("valuation-block"));
                divs.add(tabDiv.findElement(By.className("net-profit")));
                for (WebElement div : divs) {
                    WebElement label = div.findElement(By.tagName("label"));
                    WebElement span = div.findElement(By.tagName("span"));
                    data.put(label.getText(), Double.valueOf(span.getText()));
                }
            }
        } catch (Exception e) {
            initWebdriver();
            if (numTry < numTries) {
                return getZerodhaBrokerage(type, numTry + 1);
            } else {
                throw new TickerException("Error while getting values. Please try again");
            }
        }

        return data;
    }
}
