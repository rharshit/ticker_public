package com.ticker.brokerage.service;

import com.ticker.common.exception.TickerException;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.*;

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
            put(INTRADAY, Arrays.asList(INTRADAY, "i", "id", "ide", "intraday"));
            put(EQUITY, Arrays.asList(EQUITY, "e", "de", "equities"));
            put(FUTURES, Arrays.asList(FUTURES, "f", "fof", "fnof", "future", "futures"));
            put(OPTIONS, Arrays.asList(OPTIONS, "o", "foo", "fnoo", "option", "options"));
        }};
    }

    private static void initWebdriver() {
        log.info("Initializing webdriver");
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        options.addArguments("--window-size=1920,1080");
        options.addArguments("incognito");
        if (Platform.getCurrent().is(Platform.LINUX)) {
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
            options.setBinary("/usr/bin/chromium-browser");
        }
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
        throw new TickerException("Cannot find type for '" + type + "'. Valid options are -" + mapping);
    }

    public Map<String, Double> getZerodhaBrokerage(String type, String exchange,
                                                   float buy, float sell, float quantity,
                                                   int numTry) {
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
                setTabValues(tabDiv, buy, sell, quantity);
                boolean isExchangeSelected = false;
                List<WebElement> weExchanges = tabDiv.findElements(By.className("equity-radio"));
                List<String> exchanges = new ArrayList<>();
                for (WebElement weExchange : weExchanges) {
                    WebElement rb = weExchange.findElement(By.tagName("input"));
                    String exchangeValue = rb.getAttribute("value");
                    if (exchange.equalsIgnoreCase(exchangeValue)) {
                        rb.click();
                        isExchangeSelected = true;
                        break;
                    }
                    exchanges.add(exchangeValue);
                }
                if (!isExchangeSelected) {
                    throw new TickerException("Exchange value '" + exchange + "' is invalid. Valid options are: " + exchanges);
                }
                List<WebElement> divs = tabDiv.findElements(By.className("valuation-block"));
                divs.add(tabDiv.findElement(By.className("net-profit")));
                for (WebElement div : divs) {
                    WebElement label = div.findElement(By.tagName("label"));
                    WebElement span = div.findElement(By.tagName("span"));
                    data.put(convertToCamelCase(label.getText()), Double.valueOf(span.getText()));
                }
            }
        } catch (TickerException e) {
            throw e;
        } catch (Exception e) {
            initWebdriver();
            if (numTry < numTries) {
                return getZerodhaBrokerage(type, exchange, buy, sell, quantity, numTry + 1);
            } else {
                throw new TickerException("Error while getting values. Please try again");
            }
        }
        data.put("pnl", data.get("netPnl"));
        data.put("ptb", data.get("pointsToBreakeven"));
        data.put("totalBrokerage", data.get("totalTaxAndCharges"));
        return data;
    }

    private void setTabValues(WebElement tabDiv, float buy, float sell, float quantity) {
        List<WebElement> inputs = tabDiv.findElement(By.className("calc-inputs"))
                .findElements(By.className("brokerage-calculator-input"));
        for (WebElement input : inputs) {
            WebElement weLabel = input.findElement(By.tagName("label"));
            String label = weLabel.getText();
            float val = 0;
            switch (label) {
                case "BUY":
                    val = buy;
                    break;
                case "SELL":
                    val = sell;
                    break;
                case "QUANTITY":
                    val = quantity;
                    break;
                default:
                    continue;
            }
            WebElement tb = input.findElement(By.tagName("input"));
            tb.click();
            if (Platform.getCurrent().is(Platform.MAC)) {
                tb.sendKeys(Keys.COMMAND + "a");
            } else {
                tb.sendKeys(Keys.CONTROL + "a");
            }
            tb.sendKeys(String.valueOf(val));
        }
    }

    private String convertToCamelCase(String text) {
        String[] words = text.split("[ -]+");
        StringBuilder camelCase = new StringBuilder();
        for (String word : words) {
            String newWord = word.replace("&", "n");
            camelCase.append(newWord.substring(0, 1).toUpperCase()).append(newWord.substring(1).toLowerCase());
        }
        return camelCase.substring(0, 1).toLowerCase() + camelCase.substring(1);
    }
}
