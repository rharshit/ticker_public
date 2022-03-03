package com.ticker.booker.service;

import com.ticker.booker.model.CompleteTrade;
import com.ticker.booker.model.TotalTrade;
import com.ticker.booker.model.TradeGraph;
import com.ticker.booker.model.TradeMap;
import com.ticker.common.exception.TickerException;
import com.ticker.common.model.TickerTrade;
import com.ticker.common.service.BaseService;
import com.ticker.common.util.Util;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.TokenException;
import com.zerodhatech.models.Margin;
import com.zerodhatech.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ticker.booker.BookerConstants.LOG_PATH;
import static com.ticker.common.contants.TickerConstants.APPLICATION_BROKERAGE;

/**
 * The type Booker service.
 */
@Slf4j
@Service
public class BookerService extends BaseService {

    private static final List<TickerTrade> trades = new ArrayList<>();
    private static final List<CompleteTrade> completeTrades = new ArrayList<>();
    private static final Pattern pattern = Pattern.compile("^(Sold|Bought) (\\d*) (F|I|E) of (.*:.*) at (\\d\\d\\d\\d\\/\\d\\d\\/\\d\\d \\d\\d:\\d\\d:\\d\\d) for (\\d*\\.\\d*$)");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:dd");
    private static KiteConnect kiteSdk;
    private static User user;
    private static String requestToken;
    private static String apiKey;
    private static String apiSecret;
    private static String userId;
    @Autowired
    private RestTemplate restTemplate;

    private static KiteConnect getKiteSdk() {
        if (kiteSdk == null) {
            kiteSdk = new KiteConnect(apiKey);

            kiteSdk.setUserId(userId);

            kiteSdk.setSessionExpiryHook(new SessionExpiryHook() {
                @Override
                public void sessionExpired() {
                    log.warn("Kite session expired");
                }
            });
        }
        return kiteSdk;
    }

    private static void generateSession() throws IOException, KiteException {
        user = getKiteSdk().generateSession(requestToken, apiSecret);
        getKiteSdk().setAccessToken(user.accessToken);
        getKiteSdk().setPublicToken(user.publicToken);
    }

    /**
     * Gets trades.
     *
     * @return the trades
     */
    public static Map<String, Map<String, List<TickerTrade>>> getTrades() {
        Map<String, Map<String, List<TickerTrade>>> tradeMap = new HashMap<>();
        for (TickerTrade trade : trades) {
            tradeMap.computeIfAbsent(trade.getAppName(), s -> new HashMap<>());
            Map<String, List<TickerTrade>> appTradeMap = tradeMap.get(trade.getAppName());
            String key = trade.tradingSymbol + " : " + trade.exchange;
            appTradeMap.computeIfAbsent(key, s -> new ArrayList<>());
            appTradeMap.get(key).add(trade);
        }
        for (Map<String, List<TickerTrade>> appTradeMap : tradeMap.values()) {
            for (List<TickerTrade> tradeList : appTradeMap.values()) {
                tradeList.sort(Comparator.comparing(o -> o.exchangeTimestamp));
            }
        }
        return tradeMap;
    }

    /**
     * Gets properties bean.
     *
     * @param apiKey    the api key
     * @param apiSecret the api secret
     * @param userId    the user id
     */
    @Autowired
    public void getPropertiesBean(@Value("${com.zerodha.apikey}") String apiKey, @Value("${com.zerodha.apisecret}") String apiSecret, @Value("${com.zerodha.userid}") String userId) {
        BookerService.apiKey = apiKey;
        BookerService.apiSecret = apiSecret;
        BookerService.userId = userId;
    }

    /**
     * Gets zerodha login url.
     *
     * @return the zerodha login url
     */
    public String getZerodhaLoginURL() {
        return getKiteSdk().getLoginURL();
    }

    /**
     * Sets request token.
     *
     * @param requestToken the request token
     */
    public void setRequestToken(String requestToken) {
        BookerService.requestToken = requestToken;
        try {
            generateSession();
        } catch (KiteException | IOException e) {
            log.error("Error while generating session", e);
        }
    }

    /**
     * Gets zerodha margins.
     *
     * @return the zerodha margins
     */
    public Map<String, Margin> getZerodhaMargins() {
        try {
            return getKiteSdk().getMargins();
        } catch (TokenException e) {
            log.error("Error in token while getting margins");
            log.error(e.message);
            throw new TickerException("Error in token while getting Margins");
        } catch (KiteException | Exception e) {
            log.error("Error while getting margins", e);
            throw new TickerException("Error while getting Margins");
        }
    }

    /**
     * Book regular order integer.
     *
     * @param tradingSymbol     the trading symbol
     * @param exchange          the exchange
     * @param transactionType   the transaction type
     * @param orderType         the order type
     * @param quantity          the quantity
     * @param product           the product
     * @param price             the price
     * @param triggerPrice      the trigger price
     * @param disclosedQuantity the disclosed quantity
     * @param validity          the validity
     * @param tag               the tag
     * @return the integer
     */
    public Integer bookRegularOrder(String tradingSymbol, String exchange, String transactionType, String orderType,
                                    Integer quantity, String product, Double price, Double triggerPrice,
                                    Integer disclosedQuantity, String validity, String tag) {
        return null;
    }

    /**
     * Populate logs.
     *
     * @param logs the logs
     * @param app  the app
     */
    public void populateLogs(String logs, String app) {
        String[] lines = logs.split("\n");
        for (String line : lines) {
            try {
                if (line.contains("StratTickerService")) {
                    String val = line.split("StratTickerService", 2)[1].split(":", 2)[1].trim();
                    Matcher matcher = pattern.matcher(val);
                    if (matcher.find()) {
                        TickerTrade trade = new TickerTrade();
                        trade.setAppName(app == null ? "Booker" : app);

                        String transactionType = matcher.group(1);
                        if ("Bought".equalsIgnoreCase(transactionType)) {
                            trade.transactionType = "BUY";
                        } else if ("Sold".equalsIgnoreCase(transactionType)) {
                            trade.transactionType = "SELL";
                        } else {
                            new TickerException("Wrong transaction type: " + transactionType);
                        }

                        trade.quantity = String.valueOf(Integer.parseInt(matcher.group(2)));

                        String product = matcher.group(3);
                        if ("F".equalsIgnoreCase(product)) {
                            trade.exchange = "NFO";
                            trade.product = "MIS";
                        } else if ("I".equalsIgnoreCase(product)) {
                            trade.product = "MIS";
                        } else if ("E".equalsIgnoreCase(product)) {
                            trade.product = "CNC";
                        }

                        String exchangeSymbol = matcher.group(4);
                        trade.exchange = !"NFO".equalsIgnoreCase(trade.exchange) ? exchangeSymbol.split(":")[0] : "NFO";
                        trade.tradingSymbol = exchangeSymbol.split(":")[1];

                        String sDate = matcher.group(5);
                        trade.fillTimestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(sDate);
                        trade.exchangeTimestamp = trade.fillTimestamp;

                        trade.averagePrice = String.valueOf(Double.parseDouble(matcher.group(6)));

                        trades.add(trade);
                    }
                }
            } catch (Exception e) {
                log.error(line);
                log.error("Error in line", e);
            }
        }
        log.info("Added trades from log");
    }

    /**
     * Gets total trade.
     *
     * @return the total trade
     */
    public TotalTrade getTotalTrade() {
        TotalTrade totalTrade = new TotalTrade();
        totalTrade.setTradeMap(getTradeMapObject());
        totalTrade.setTradeGraph(getTradeGraph(5));
        return totalTrade;
    }

    private TradeGraph getTradeGraph(int interval) {
        TradeGraph tradeGraph = new TradeGraph();
        String date = (new SimpleDateFormat("MMM dd yyyy ")).format(new Date(System.currentTimeMillis()));
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm");
        double totalNetPnl = 0;
        double totalTaxes = 0;
        int i = 0;
        for (int h = 9; h < 16 && i < completeTrades.size(); h++) {
            for (int m = 0; m < 60 && i < completeTrades.size(); m += interval) {
                String time = (h < 10 ? "0" + h : h) + ":" + (m < 10 ? "0" + m : m);
                String dateTime = date + time;
                try {
                    long millis = sdf.parse(dateTime).getTime();
                    while (i < completeTrades.size() && completeTrades.get(i).getTimestamp().getTime() < millis) {
                        totalNetPnl += completeTrades.get(i).getPnl();
                        totalTaxes += completeTrades.get(i).getTaxes();
                        i++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                tradeGraph.getLabels().add(time);
                tradeGraph.getNetPnl().add(totalNetPnl);
                tradeGraph.getTaxes().add(totalTaxes);
                tradeGraph.getPnl().add(totalNetPnl + totalTaxes);
                tradeGraph.getNumTrades().add(i);
            }
        }
        return tradeGraph;
    }

    private TradeMap getTradeMapObject() {
        try {
            log.info("Getting trade map");
            Map<String, Map<String, Map<String, Map<String, List<TickerTrade>>>>> tradeMap = getTradeMap();
            Map<String, Map<String, Map<String, Map<String, List<CompleteTrade>>>>> completeTradeMap = processTradeMap(tradeMap);
            TradeMap totalTrade = new TradeMap(completeTradeMap);
            log.info("Got trade map");
            return totalTrade;
        } catch (Exception e) {
            log.error("Error while getting trades", e);
            throw new TickerException(e.getMessage());
        }
    }

    private Map<String, Map<String, Map<String, Map<String, List<CompleteTrade>>>>> processTradeMap(Map<String, Map<String, Map<String, Map<String, List<TickerTrade>>>>> appMap) {
        List<CompleteTrade> completeTradesTemp = new ArrayList<>();
        Map<String, Map<String, Map<String, Map<String, List<CompleteTrade>>>>> completeAppMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Map<String, List<TickerTrade>>>>> appEntry : appMap.entrySet()) {
            String appName = appEntry.getKey();
            completeAppMap.computeIfAbsent(appName, s -> new HashMap<>());

            Map<String, Map<String, Map<String, List<CompleteTrade>>>> completeTradeMap = completeAppMap.get(appName);

            for (Map.Entry<String, Map<String, Map<String, List<TickerTrade>>>> tradeEntry : appEntry.getValue().entrySet()) {
                String symbol = tradeEntry.getKey();
                completeTradeMap.computeIfAbsent(symbol, s -> new HashMap<>());

                Map<String, Map<String, List<CompleteTrade>>> completeSymbolMap = completeTradeMap.get(symbol);

                for (Map.Entry<String, Map<String, List<TickerTrade>>> symbolEntry : tradeEntry.getValue().entrySet()) {
                    String exchange = symbolEntry.getKey();
                    completeSymbolMap.computeIfAbsent(exchange, s -> new HashMap<>());

                    Map<String, List<CompleteTrade>> completeProductMap = completeSymbolMap.get(exchange);

                    for (Map.Entry<String, List<TickerTrade>> productEntry : symbolEntry.getValue().entrySet()) {
                        String product = productEntry.getKey();
                        completeProductMap.computeIfAbsent(product, s -> new ArrayList<>());

                        List<CompleteTrade> completeTradeList = completeProductMap.get(product);

                        productEntry.getValue().sort(Comparator.comparing(o -> o.fillTimestamp));

                        LinkedList<TickerTrade> buyTradeList = new LinkedList<>();
                        LinkedList<TickerTrade> sellTradeList = new LinkedList<>();

                        productEntry.getValue().sort(Comparator.comparing(o -> o.fillTimestamp));

                        for (TickerTrade trade : productEntry.getValue()) {
                            if ("BUY".equalsIgnoreCase(trade.transactionType)) {
                                buyTradeList.add(trade);
                            } else if ("SELL".equalsIgnoreCase(trade.transactionType)) {
                                sellTradeList.add(trade);
                            } else {
                                throw new TickerException("Invalid transaction type: " + trade.transactionType);
                            }
                        }

                        int balanceQty = 0;
                        TickerTrade balance = new TickerTrade();
                        balance.quantity = String.valueOf(0);
                        balance.averagePrice = String.valueOf(0);
                        for (; (!buyTradeList.isEmpty() && !sellTradeList.isEmpty()) ||
                                (buyTradeList.isEmpty() && !sellTradeList.isEmpty() && Integer.parseInt(balance.quantity) < 0) ||
                                (!buyTradeList.isEmpty() && sellTradeList.isEmpty() && Integer.parseInt(balance.quantity) > 0); ) {
                            CompleteTrade completeTrade = new CompleteTrade();
                            TickerTrade buy = null;
                            TickerTrade sell = null;

                            if (Integer.parseInt(balance.quantity) == 0) {
                                buy = buyTradeList.pop();
                                sell = sellTradeList.pop();
                            } else if (Integer.parseInt(balance.quantity) > 0) {
                                sell = sellTradeList.pop();
                                buy = new TickerTrade(balance);
                                if (Integer.parseInt(balance.quantity) == Integer.parseInt(sell.quantity)) {
                                    balance.quantity = String.valueOf(0);
                                    balance.averagePrice = String.valueOf(0);
                                } else if (Integer.parseInt(balance.quantity) > Integer.parseInt(sell.quantity)) {
                                    buy.quantity = sell.quantity;
                                    balance.quantity = String.valueOf((Integer.parseInt(balance.quantity) - Integer.parseInt(sell.quantity)));
                                } else {
                                    balance = new TickerTrade(sell);
                                    balance.quantity = String.valueOf(-Integer.parseInt(balance.quantity));
                                    sell.quantity = buy.quantity;
                                }
                            } else if (Integer.parseInt(balance.quantity) < 0) {
                                buy = buyTradeList.pop();
                                sell = new TickerTrade(balance);
                                sell.quantity = String.valueOf(-Integer.parseInt(sell.quantity));
                                if (-Integer.parseInt(balance.quantity) == Integer.parseInt(buy.quantity)) {
                                    balance.quantity = String.valueOf(0);
                                    balance.averagePrice = String.valueOf(0);
                                } else if (-Integer.parseInt(balance.quantity) > Integer.parseInt(buy.quantity)) {
                                    sell.quantity = buy.quantity;
                                    balance.quantity = String.valueOf((Integer.parseInt(balance.quantity) + Integer.parseInt(buy.quantity)));
                                } else {
                                    balance = new TickerTrade(buy);
                                    buy.quantity = sell.quantity;
                                }
                            }

                            if (buy == null || sell == null) {
                                throw new TickerException("Internal error: Buy or sell trade nto set");
                            }

                            int tradeQuantity = Math.min(Integer.parseInt(buy.quantity), Integer.parseInt(sell.quantity));
                            if (Integer.parseInt(buy.quantity) != Integer.parseInt(sell.quantity)) {
                                if (Integer.parseInt(balance.quantity) != 0) {
                                    throw new TickerException("Internal error: Error in trades");
                                }
                                if (Integer.parseInt(buy.quantity) > Integer.parseInt(sell.quantity)) {
                                    balance = new TickerTrade(buy);
                                } else {
                                    balance = new TickerTrade(sell);
                                }
                                balance.quantity = String.valueOf(Integer.parseInt(buy.quantity) - Integer.parseInt(sell.quantity));
                            }

                            completeTrade.setAppName(appName);
                            completeTrade.setExchange(exchange);
                            completeTrade.setBuy(buy);
                            completeTrade.setSell(sell);
                            completeTrade.setQuantity(tradeQuantity);
                            completeTrade.setCompleted(true);

                            if (validTrade(completeTrade)) {
                                completeTradeList.add(completeTrade);
                                completeTradesTemp.add(completeTrade);
                            } else {
                                log.info("Skipping invalid/incomplete trade");
                            }
                        }
                    }
                }
            }
        }

        List<Thread> threads = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, Map<String, List<TickerTrade>>>>> appEntry : appMap.entrySet()) {
            String appName = appEntry.getKey();
            completeAppMap.computeIfAbsent(appName, s -> new HashMap<>());

            Map<String, Map<String, Map<String, List<CompleteTrade>>>> completeTradeMap = completeAppMap.get(appName);

            for (Map.Entry<String, Map<String, Map<String, List<TickerTrade>>>> tradeEntry : appEntry.getValue().entrySet()) {
                String symbol = tradeEntry.getKey();
                completeTradeMap.computeIfAbsent(symbol, s -> new HashMap<>());

                Map<String, Map<String, List<CompleteTrade>>> completeSymbolMap = completeTradeMap.get(symbol);

                for (Map.Entry<String, Map<String, List<TickerTrade>>> symbolEntry : tradeEntry.getValue().entrySet()) {
                    String exchange = symbolEntry.getKey();
                    completeSymbolMap.computeIfAbsent(exchange, s -> new HashMap<>());

                    Map<String, List<CompleteTrade>> completeProductMap = completeSymbolMap.get(exchange);

                    for (Map.Entry<String, List<TickerTrade>> productEntry : symbolEntry.getValue().entrySet()) {
                        String product = productEntry.getKey();
                        completeProductMap.computeIfAbsent(product, s -> new ArrayList<>());

                        List<CompleteTrade> completeTradeList = completeProductMap.get(product);
                        for (CompleteTrade completeTrade : completeTradeList) {
                            Thread thread = new Thread(() -> processTrade(completeTrade, symbol, exchange, product));
                            thread.start();
                            threads.add(thread);
                        }
                    }
                }
            }
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        completeTradesTemp.sort(Comparator.comparing(CompleteTrade::getTimestamp));
        completeTrades.clear();
        completeTrades.addAll(completeTradesTemp);
        return completeAppMap;
    }

    private boolean validTrade(CompleteTrade completeTrade) {
        try {
            if (completeTrade.getSell() == null
                    || completeTrade.getSell().quantity == null
                    || Integer.parseInt(completeTrade.getSell().quantity) == 0
                    || completeTrade.getBuy() == null
                    || completeTrade.getBuy().quantity == null
                    || Integer.parseInt(completeTrade.getBuy().quantity) == 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Map<String, Map<String, Map<String, Map<String, List<TickerTrade>>>>> getTradeMap() {
        Map<String, Map<String, Map<String, Map<String, List<TickerTrade>>>>> appMap = new HashMap<>();
        for (TickerTrade trade : trades) {
            String appName = trade.getAppName();
            String symbol = trade.tradingSymbol;
            String exchange = trade.exchange;
            String product = trade.product;

            appMap.computeIfAbsent(appName, s -> new HashMap<>());
            Map<String, Map<String, Map<String, List<TickerTrade>>>> tradeMap = appMap.get(appName);

            tradeMap.computeIfAbsent(symbol, s -> new HashMap<>());
            Map<String, Map<String, List<TickerTrade>>> symbolMap = tradeMap.get(symbol);

            symbolMap.computeIfAbsent(exchange, s -> new HashMap<>());
            Map<String, List<TickerTrade>> productMap = symbolMap.get(exchange);

            productMap.computeIfAbsent(product, s -> new ArrayList<>());
            List<TickerTrade> tradeList = productMap.get(product);

            tradeList.add(trade);
        }
        return appMap;
    }

    private void processTrade(CompleteTrade trade, String symbol, String exchange, String product) {
        trade.setSymbol(symbol);
        trade.setProduct(product);
        if ("NFO".equalsIgnoreCase(exchange)) {
            trade.setExchange("NSE");
            trade.setTickerType("F");
        } else {
            if ("MIS".equalsIgnoreCase(product)) {
                trade.setTickerType("I");
            } else {
                trade.setTickerType("E");
            }
        }
        log.debug("Fetching brokerage");
        Map<String, Double> brokerage = getBrokerage(trade);
        log.debug("Fetched brokerage");
        trade.setPnl(brokerage.get("netPnl").doubleValue());
        trade.setTaxes(brokerage.get("totalBrokerage").doubleValue());
    }

    /**
     * Gets brokerage.
     *
     * @param trade the trade
     * @return the brokerage
     */
    public Map<String, Double> getBrokerage(CompleteTrade trade) {
        String url = Util.getApplicationUrl(APPLICATION_BROKERAGE) +
                "zerodha/" +
                trade.getTickerType() + "/" +
                trade.getExchange();
        Map<String, Object> params = new HashMap<>();
        params.put("buy", trade.getBuy().averagePrice);
        params.put("sell", trade.getSell().averagePrice);
        params.put("quantity", trade.getQuantity());
        return restTemplate.getForObject(url, Map.class, params);
    }

    /**
     * Gets log files.
     *
     * @return the log files
     */
    public List<String> getLogFiles() {
        List<String> files = new ArrayList<>();
        final File folder = new File(LOG_PATH);
        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                String name = fileEntry.getName();
                if (name.endsWith(".log")) {
                    files.add(name);
                }
            }
        }
        files.sort(String::compareTo);
        return files;
    }

    /**
     * Upload log file.
     *
     * @param file the file
     */
    public void uploadLogFile(String file) {
        String path = LOG_PATH + "/" + file;
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            log.error("Error while reading file at path: " + path);
        }
        String log = new String(encoded, StandardCharsets.US_ASCII);
        populateLogs(log, file.replaceAll("\\.log$", ""));
    }

    /**
     * Upload all log files.
     */
    public void uploadAllLogFiles() {
        List<String> files = getLogFiles();
        for (String file : files) {
            uploadLogFile(file);
        }
    }

    /**
     * Delete logs.
     */
    public void deleteLogs() {
        trades.clear();
    }

    @Override
    protected Map<String, Executor> getExecutorMap() {
        return new HashMap<>();
    }
}
