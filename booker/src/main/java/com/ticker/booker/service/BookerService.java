package com.ticker.booker.service;

import com.ticker.booker.model.CompleteTrade;
import com.ticker.booker.model.TradeMap;
import com.ticker.common.exception.TickerException;
import com.ticker.common.model.TickerTrade;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ticker.booker.BookerConstants.LOG_PATH;
import static com.ticker.common.contants.TickerConstants.APPLICATION_BROKERAGE;

@Slf4j
@Service
public class BookerService {

    @Autowired
    private RestTemplate restTemplate;

    private static KiteConnect kiteSdk;
    private static User user;
    private static String requestToken;

    private static String apiKey;
    private static String apiSecret;
    private static String userId;

    private static final List<TickerTrade> trades = new ArrayList<>();

    private static final Pattern pattern = Pattern.compile("^(Sold|Bought) (\\d*) (F|I|E) of (.*:.*) at (\\d\\d\\d\\d\\/\\d\\d\\/\\d\\d \\d\\d:\\d\\d:\\d\\d) for (\\d*\\.\\d*$)");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:dd");


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

    @Autowired
    public void getPropertiesBean(@Value("${com.zerodha.apikey}") String apiKey, @Value("${com.zerodha.apisecret}") String apiSecret, @Value("${com.zerodha.userid}") String userId) {
        BookerService.apiKey = apiKey;
        BookerService.apiSecret = apiSecret;
        BookerService.userId = userId;
    }

    public String getZerodhaLoginURL() {
        return getKiteSdk().getLoginURL();
    }

    public void setRequestToken(String requestToken) {
        BookerService.requestToken = requestToken;
        try {
            generateSession();
        } catch (KiteException | IOException e) {
            log.error("Error while generating session", e);
        }
    }

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

    public Integer bookRegularOrder(String tradingSymbol, String exchange, String transactionType, String orderType,
                                    Integer quantity, String product, Float price, Float triggerPrice,
                                    Integer disclosedQuantity, String validity, String tag) {
        return null;
    }

    public static List<TickerTrade> getTrades() {
        List<TickerTrade> allTrades = new ArrayList<>(trades);
        allTrades.sort(Comparator.comparing(o -> o.exchangeTimestamp));
        return allTrades;
    }

    public void populateLogs(String logs) {
        String[] lines = logs.split("\n");
        for (String line : lines) {
            try {
                if (line.contains("StratTickerService")) {
                    String val = line.split("StratTickerService", 2)[1].split(":", 2)[1].trim();
                    Matcher matcher = pattern.matcher(val);
                    if (matcher.find()) {
                        TickerTrade trade = new TickerTrade();
                        trade.setAppName("Booker");

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

                        trade.averagePrice = String.valueOf(Float.parseFloat(matcher.group(6)));

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

    public TradeMap getTotalTrade() {
        try {
            log.info("Getting total trade");
            Map<String, Map<String, Map<String, List<TickerTrade>>>> tradeMap = getTradeMap();
            Map<String, Map<String, Map<String, List<CompleteTrade>>>> completeTradeMap = processTradeMap(tradeMap);
            TradeMap totalTrade = new TradeMap(completeTradeMap);
            log.info("Got total trade");
            return totalTrade;
        } catch (Exception e) {
            log.error("Error while getting trades", e);
            throw new TickerException(e.getMessage());
        }

    }

    private Map<String, Map<String, Map<String, List<CompleteTrade>>>> processTradeMap(Map<String, Map<String, Map<String, List<TickerTrade>>>> tradeMap) {
        Map<String, Map<String, Map<String, List<CompleteTrade>>>> completeTradeMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, List<TickerTrade>>>> tradeEntry : tradeMap.entrySet()) {
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

                        completeTrade.setExchange(exchange);
                        completeTrade.setBuy(buy);
                        completeTrade.setSell(sell);
                        completeTrade.setQuantity(tradeQuantity);
                        completeTrade.setCompleted(true);


                        completeTradeList.add(completeTrade);
                    }
                }
            }
        }

        List<Thread> threads = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, List<TickerTrade>>>> tradeEntry : tradeMap.entrySet()) {
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
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return completeTradeMap;
    }

    private Map<String, Map<String, Map<String, List<TickerTrade>>>> getTradeMap() {
        Map<String, Map<String, Map<String, List<TickerTrade>>>> tradeMap = new HashMap<>();
        for (TickerTrade trade : trades) {
            String symbol = trade.tradingSymbol;
            String exchange = trade.exchange;
            String product = trade.product;

            tradeMap.computeIfAbsent(symbol, s -> new HashMap<>());
            Map<String, Map<String, List<TickerTrade>>> symbolMap = tradeMap.get(symbol);

            symbolMap.computeIfAbsent(exchange, s -> new HashMap<>());
            Map<String, List<TickerTrade>> productMap = symbolMap.get(exchange);

            productMap.computeIfAbsent(product, s -> new ArrayList<>());
            List<TickerTrade> tradeList = productMap.get(product);

            tradeList.add(trade);
        }
        return tradeMap;
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
        trade.setPnl(brokerage.get("pnl").floatValue());
        trade.setTaxes(brokerage.get("totalBrokerage").floatValue());
    }

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

    public void uploadLogFile(String file) {
        String path = LOG_PATH + "/" + file;
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            log.error("Error while reading file at path: " + path);
        }
        String log = new String(encoded, StandardCharsets.US_ASCII);
        populateLogs(log);
    }

    public void uploadAllLogFiles() {
        List<String> files = getLogFiles();
        for (String file : files) {
            uploadLogFile(file);
        }
    }
}
