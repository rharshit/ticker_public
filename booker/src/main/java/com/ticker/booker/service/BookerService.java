package com.ticker.booker.service;

import com.ticker.booker.model.CompleteTrade;
import com.ticker.booker.model.TotalTrade;
import com.ticker.common.exception.TickerException;
import com.ticker.common.model.TickerTrade;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BookerService {

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
        return trades;
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

                        trade.fillTimestamp = sdf.parse(matcher.group(5));
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

    public TotalTrade getTotalTrade() {
        TotalTrade totalTrade = new TotalTrade();
        Map<String, Map<String, Map<String, List<TickerTrade>>>> tradeMap = getTradeMap();
        Map<String, Map<String, Map<String, List<CompleteTrade>>>> completeTradeMap = processTradeMap(tradeMap);
        totalTrade.setCompleteTradeMap(completeTradeMap);

        return totalTrade;
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

                    if (Math.abs(buyTradeList.size() - sellTradeList.size()) > 1) {
                        //TODO: Change it based on number of applications accessing
                        throw new TickerException("Mismatched buy and sell queue");
                    }

                    int balanceQty = 0;
                    for (int i = 0; i < Math.min(buyTradeList.size(), sellTradeList.size()); i++) {
                        TickerTrade buyTrade = buyTradeList.pop();
                        TickerTrade sellTrade = sellTradeList.pop();

                        int buyQty = Integer.parseInt(buyTrade.quantity);
                        int sellQty = Integer.parseInt(sellTrade.quantity);

                        if (balanceQty < 0) {
                            sellQty = sellQty - balanceQty;
                            balanceQty = 0;
                        } else if (balanceQty > 0) {
                            buyQty = buyQty + balanceQty;
                            balanceQty = 0;
                        }
                        int tradeQty = Math.min(buyQty, sellQty);
                        balanceQty = balanceQty + buyQty - sellQty;
                        CompleteTrade completeTrade = new CompleteTrade();
                        completeTrade.setQuantity(tradeQty);

                        TickerTrade buyTickerTrade = new TickerTrade(buyTrade);
                        buyTickerTrade.quantity = String.valueOf(tradeQty);
                        completeTrade.setBuy(buyTickerTrade);

                        TickerTrade sellTickerTrade = new TickerTrade(sellTrade);
                        sellTickerTrade.quantity = String.valueOf(tradeQty);
                        completeTrade.setSell(sellTickerTrade);

                        completeTrade.setCompleted(true);

                        completeTradeList.add(completeTrade);
                    }
                }
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

}
