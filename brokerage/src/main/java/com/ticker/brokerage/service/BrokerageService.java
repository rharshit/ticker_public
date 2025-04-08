package com.ticker.brokerage.service;

import com.ticker.brokerage.objectpool.ZerodhaWebdriverPoolData;
import com.ticker.common.exception.TickerException;
import com.ticker.common.service.BaseService;
import com.ticker.common.util.objectpool.ObjectPool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.ticker.brokerage.constants.WebConstants.ZERODHA_BROKERAGE_URL;
import static com.ticker.common.util.Util.*;


/**
 * The type Brokerage service.
 */
@Service
@Slf4j
public class BrokerageService extends BaseService {

    /**
     * The constant numTries.
     */
    public static final int NUM_TRIES = 3;
    private static final String EQUITY = "equity";
    private static final String INTRADAY = "intraday";
    private static final String FUTURES = "futures";
    private static final String OPTIONS = "options";
    private static final Map<String, List<String>> tabs; //TODO: Use enum instead
    private static final ObjectPool<ZerodhaWebdriverPoolData> zerodhaWebdrivers;
    /**
     * -- GETTER --
     * Is busy boolean.
     *
     * @return the boolean
     */
    @Getter
    private static boolean busy = false;
    private static final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    static {
        zerodhaWebdrivers = new ObjectPool<ZerodhaWebdriverPoolData>(20, 40, 50, 1000, 180000) {
            @Override
            public ZerodhaWebdriverPoolData createObject() {
                return new ZerodhaWebdriverPoolData(ZERODHA_BROKERAGE_URL);
            }
        };

        tabs = new HashMap<String, List<String>>() {{
            put(INTRADAY, Arrays.asList(INTRADAY, "i", "id", "ide", "intraday"));
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
        throw new TickerException("Cannot find type for '" + type + "'. Valid options are -" + mapping);
    }

    /**
     * Gets zerodha brokerage.
     *
     * @param type     the type
     * @param exchange the exchange
     * @param buy      the buy
     * @param sell     the sell
     * @param quantity the quantity
     * @param numTry   the num try
     * @return the zerodha brokerage
     */
    private static Map<String, Double> getZerodhaBrokerage(String type, String exchange,
                                                           double buy, double sell, double quantity,
                                                   int numTry) {
        final boolean[] fetched = {false};
        final boolean[] error = {false};
        final boolean[] isExchangeSelected = {false};
        log.debug("start: " + exchange + " : " + buy + ", " + sell + ", " + quantity);
        long start = System.currentTimeMillis();
        Map<String, Double> data = new HashMap<>();
        executor.execute(() -> {
            try {
                busy = true;
                WebDriver webDriver;
                long startTime = System.currentTimeMillis();
                boolean retry = false;
                while (true) {
                    if (System.currentTimeMillis() - startTime > 100000) {
                        throw new TickerException("Error getting webdriver, closing");
                    }
                    log.debug("Getting webdriver");
                    webDriver = (WebDriver) zerodhaWebdrivers.get(retry);
                    if (webDriver != null) {
                        break;
                    } else {
                        retry = true;
                    }
                    waitFor(WAIT_SHORT);
                }
                log.debug("Got webdriver");
                long startFetch = System.currentTimeMillis();
                String tabType = getTabType(type);
                log.debug(tabType);
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
                try {
                    WebElement tabDiv = webDriver.findElement(By.id(divId));
                    setTabValues(tabDiv, buy, sell, quantity);
                    List<WebElement> weExchanges = tabDiv.findElements(By.className("equity-radio"));
                    List<String> exchanges = new ArrayList<>();
                    for (WebElement weExchange : weExchanges) {
                        WebElement rb = weExchange.findElement(By.tagName("input"));
                        String exchangeValue = rb.getAttribute("value");
                        if (exchange.equalsIgnoreCase(exchangeValue)) {
                            rb.click();
                            isExchangeSelected[0] = true;
                            break;
                        }
                        exchanges.add(exchangeValue);
                    }
                    if (!isExchangeSelected[0]) {
                        throw new TickerException("Exchange value '" + exchange + "' is invalid. Valid options are: " + exchanges);
                    }
                    List<WebElement> divs = tabDiv.findElements(By.className("valuation-block"));
                    divs.add(tabDiv.findElement(By.className("net-profit")));
                    for (WebElement div : divs) {
                        WebElement label = div.findElement(By.tagName("label"));
                        WebElement span = div.findElement(By.tagName("span"));
                        data.put(convertToCamelCase(label.getText()), Double.valueOf(span.getText()));
                    }
                } catch (TickerException e) {
                    throw e;
                } catch (Exception e) {
                    error[0] = true;
                } finally {
                    zerodhaWebdrivers.put(webDriver);
                    busy = false;
                }
                data.put("pnl", data.get("netPnl"));
                data.put("ptb", data.get("pointsToBreakeven"));
                data.put("totalBrokerage", data.get("totalTaxAndCharges"));
                fetched[0] = true;
                log.info("Time taken - Webdriver: {}ms, Fetch: {}ms, total: {}ms",
                        startFetch - start, System.currentTimeMillis() - startFetch, System.currentTimeMillis() - start);
            } catch (Exception e) {
                fetched[0] = false;
                error[0] = true;
            }

        });
        while (!fetched[0] && !error[0]) {
            waitFor(WAIT_QUICK);
        }
        if (fetched[0]) {
            return data;
        } else {
            if (numTry < NUM_TRIES && isExchangeSelected[0]) {
                log.info("Error while getting brokerage, retrying {}", numTry);
                return getZerodhaBrokerage(type, exchange, buy, sell, quantity, numTry + 1);
            } else {
                log.error("Error while getting values: {} {} {} {} {} {}", type, exchange, buy, sell, quantity, numTry);
                throw new TickerException("Error while getting values. Please try again");
            }
        }
    }

    private static void setTabValues(WebElement tabDiv, double buy, double sell, double quantity) {
        List<WebElement> inputs = tabDiv.findElement(By.className("calc-inputs"))
                .findElements(By.className("brokerage-calculator-input"));
        for (WebElement input : inputs) {
            WebElement weLabel = input.findElement(By.tagName("label"));
            String label = weLabel.getText();
            double val;
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

    private static String convertToCamelCase(String text) {
        String[] words = text.split("[ -]+");
        StringBuilder camelCase = new StringBuilder();
        for (String word : words) {
            String newWord = word.replace("&", "n");
            camelCase.append(newWord.substring(0, 1).toUpperCase()).append(newWord.substring(1).toLowerCase());
        }
        return camelCase.substring(0, 1).toLowerCase() + camelCase.substring(1);
    }

    /**
     * Get zerodha webdriver pool size int [ ].
     *
     * @return the int [ ]
     */
    public static int[] getZerodhaWebdriverPoolSize() {
        return zerodhaWebdrivers.poolSize();
    }

    @Cacheable("brokerage")
    public Map<String, Double> getZerodhaBrokerageWrapper(String type, String exchange,
                                                          double buy, double sell, double quantity) {
        try {
            return getZerodhaBrokerage(type, exchange, buy, sell, quantity, 0);
        } catch (Exception e) {
            return new HashMap<String, Double>() {{
                put("pnl", 0.0);
                put("netPnl", 0.0);
                put("ptb", 0.0);
                put("totalBrokerage", 0.0);
            }};
        }
    }

    @Override
    protected Map<String, Executor> getExecutorMap() {
        return new HashMap<>();
    }
}
