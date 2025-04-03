package com.ticker.fetcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticker.common.exception.TickerException;
import com.ticker.common.service.BaseService;
import com.ticker.fetcher.model.FetcherRepoModel;
import com.ticker.fetcher.model.websocket.response.*;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.rx.FetcherThread;
import com.ticker.fetcher.utils.compute.ComputeEngine;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ticker.common.util.Util.WAIT_QUICK;
import static com.ticker.common.util.Util.waitFor;
import static com.ticker.fetcher.rx.FetcherThread.STUDY_SERIES_CODE;
import static com.ticker.fetcher.utils.FetcherUtil.decodeMessage;
import static com.ticker.fetcher.utils.FetcherUtil.encodeMessage;

/**
 * The type Fetcher service.
 */
@Service
@Slf4j
public class FetcherService extends BaseService {

    private static final Set<FetcherRepoModel> dataSet = new HashSet<>();

    static {
        FetcherThread.fetchBuildTime();
    }

    @Autowired
    private TickerService appService;
    @Autowired
    private FetcherAppRepository repository;
    @Autowired
    @Qualifier("fetcherTaskExecutor")
    private Executor fetcherTaskExecutor;
    @Autowired
    @Qualifier("scheduledExecutor")
    private Executor scheduledExecutor;
    @Autowired
    @Qualifier("repoExecutor")
    private Executor repoExecutor;

    private static final Set<WebSocketClient> activeWebsockets = new HashSet<>();
    private static final Set<WebSocketClient> websocketsToRemove = new HashSet<>();

    public static synchronized void addToActiveWebsockets(WebSocketClient webSocketClient) {
        synchronized (activeWebsockets) {
            activeWebsockets.add(webSocketClient);
        }
    }

    public static void closeWebSocket(WebSocketClient webSocketClient) {
        if (webSocketClient != null) {
            synchronized (websocketsToRemove) {
                websocketsToRemove.add(webSocketClient);
            }
        }
    }

    public static void closeWebSockets(List<WebSocketClient> webSocketClients) {
        synchronized (websocketsToRemove) {
            websocketsToRemove.addAll(webSocketClients.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }
    }

    /**
     * Send message.
     *
     * @param thread the thread
     * @param data   the data
     */
    public void sendMessage(FetcherThread thread, String data) {
        if (thread.getWebSocketClient() != null && thread.getWebSocketClient().isOpen()) {
            log.trace("{} : sending message\n{}", thread.getThreadName(), data);
            sendMessage(thread.getWebSocketClient(), data);
            thread.getWebSocketClient().send(encodeMessage(data));
            waitFor(5);
        } else {
            log.warn(thread.getThreadName() + " : Cannot send message, websocket not open");
        }
    }

    /**
     * Send message.
     *
     * @param webSocket the web socket
     * @param data      the data
     */
    public void sendMessage(WebSocketClient webSocket, String data) {
        log.trace("{} - Sending message", webSocket);
        if (webSocket != null && webSocket.isOpen()) {
            log.trace("Sending message : {}", data);
            webSocket.send(encodeMessage(data));
            waitFor(5);
        }
    }

    /**
     * On message received.
     *
     * @param thread    the thread
     * @param webSocket the web socket
     * @param data      the data
     */
    public void onReceiveMessage(FetcherThread thread, WebSocketClient webSocket, String data) {
        log.trace("{} - Message received", webSocket);
        String[] messages = decodeMessage(data);
        log.trace("Receive : {}\n{}", thread.getThreadName(), String.join("\n", messages));
        for (String message : messages) {
            if (Pattern.matches("~h~\\d*$", message)) {
                sendMessage(webSocket, message);
                log.debug("Replying : {} - {}", thread.getThreadName(), message);
            } else {
                parseMessage(thread, message);
            }
        }
    }

    private void parseMessage(FetcherThread thread, String message) {
        try {
            JSONObject object = new JSONObject(message);
            if (object.has("session_id")) {
                thread.setSessionId(object.getString("session_id"));
            } else if (object.has("p")) {
                JSONArray array = object.getJSONArray("p");
                for (int i = 0; i < array.length(); i++) {
                    try {
                        String objString = array.get(i).toString();
                        JSONObject jsonObject = new JSONObject(objString);
                        if (jsonObject.has(STUDY_SERIES_CODE) || jsonObject.has("s") || jsonObject.has("v")) {
                            decodeValues(jsonObject, thread);
                        }
                    } catch (Exception ignored) {

                    }
                }
            }
        } catch (Exception ignore) {

        }
    }

    private void decodeValues(JSONObject jsonObject, FetcherThread thread) {
        boolean updated = false;
        boolean decoded = false;
        try {
            ObjectMapper mapper = new ObjectMapper(); //TODO: Initialize one instance

            CurrentLp currentLp = mapToObject(mapper, jsonObject, CurrentLp.class);
            CurrentOhlc currentOhlc = mapToObject(mapper, jsonObject, CurrentOhlc.class);
            PrevBars prevBars = mapToObject(mapper, jsonObject, PrevBars.class);
            DayOhlc dayOhlc = mapToObject(mapper, jsonObject, DayOhlc.class);
            TickerDetails tickerDetails = mapToObject(mapper, jsonObject, TickerDetails.class);

            if (currentLp != null) {
                if (currentLp.v != null && currentLp.v.lp != 0) {
                    updated = setCurrentLp(thread, currentLp);
                    decoded = true;
                } else {
                    log.trace("Not a currentLp type");
                }
            }
            if (currentOhlc != null) {
                if (currentOhlc.sds.s.size() == 1 && currentOhlc.sds.s.get(0).v.size() >= 5) {
                    updated = setCurrentOhlc(thread, currentOhlc);
                    decoded = true;
                } else {
                    log.trace("Not a currentOhlc type");
                }
            }
            if (prevBars != null) {
                updated = setPrevBars(thread, prevBars);
                decoded = true;
            }
            if (dayOhlc != null) {
                DayOhlc.V v = dayOhlc.v;
                if (v != null && v.lp != 0 &&
                        v.open_price != 0 && v.high_price != 0
                        && v.low_price != 0 && v.prev_close_price != 0) {
                    updated = setDayOhlc(thread, dayOhlc);
                    decoded = true;
                } else {
                    log.trace("Not a dayOhlc type");
                }
            }
            if (tickerDetails != null) {
                TickerDetails.V v = tickerDetails.v;
                if (v != null && v.lp != 0 &&
                        v.open_price != 0 && v.high_price != 0
                        && v.low_price != 0 && v.prev_close_price != 0) {
                    updated = setTickerDetails(thread, tickerDetails);
                    decoded = true;
                } else {
                    log.trace("Not a tickerDetails type");
                }
            }
        } catch (Exception e) {
            decoded = false;
            updated = false;
        }


        if (!decoded) {
            log.trace("Not decoded : {} - {}", thread.getThreadName(), jsonObject);
        } else {
            log.trace("Decoded : {} at {}", thread.getThreadName(), System.currentTimeMillis());
            if (updated) {
                thread.setUpdatedAt(System.currentTimeMillis());
                log.trace("Updated {}", thread.getThreadName());
                synchronized (dataSet) {
                    dataSet.add(new FetcherRepoModel(thread));
                }
                thread.closePreviousWebsockets();
            }
        }
    }

    private boolean setPrevBars(FetcherThread thread, PrevBars prevBars) {
        log.trace("setPrevBars : {}", prevBars);
        try {
            List<ComputeEngine.ComputeData> data = prevBars.sds.s.stream()
                    .map(s -> new ComputeEngine.ComputeData(s.v.get(4), (long) (s.v.get(0) * 1000)))
                    .collect(Collectors.toList());
            thread.addPrevBarData(data);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private boolean setTickerDetails(FetcherThread thread, TickerDetails tickerDetails) {
        log.trace("setTickerDetails : {}", tickerDetails);
        try {
            thread.setDayO(tickerDetails.v.open_price);
            thread.setDayH(tickerDetails.v.high_price);
            thread.setDayL(tickerDetails.v.low_price);
            thread.setDayC(tickerDetails.v.lp);
            thread.setC(tickerDetails.v.lp);
            thread.setPrevClose(tickerDetails.v.prev_close_price);
            thread.setLastDailyValueUpdatedAt(System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setDayOhlc(FetcherThread thread, DayOhlc dayOhlc) {
        log.trace("dayOhlc : {}", dayOhlc);
        try {
            thread.setDayO(dayOhlc.v.open_price);
            thread.setDayH(dayOhlc.v.high_price);
            thread.setDayL(dayOhlc.v.low_price);
            thread.setDayC(dayOhlc.v.lp);
            thread.setC(dayOhlc.v.lp);
            thread.setPrevClose(dayOhlc.v.prev_close_price);
            thread.setLastDailyValueUpdatedAt(System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setCurrentOhlc(FetcherThread thread, CurrentOhlc currentOhlc) {
        log.trace("currentOhlc : ({}) {}", currentOhlc.sds.s.size(), currentOhlc);
        try {
            List<Double> vals = currentOhlc.sds.s.get(0).v;
            if (vals == null) {
                return false;
            }
            thread.setO(vals.get(1));
            thread.setH(vals.get(2));
            thread.setL(vals.get(3));
            thread.setC(vals.get(4));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setCurrentLp(FetcherThread thread, CurrentLp currentLp) {
        log.trace("currentLp : {}", currentLp);
        try {
            thread.setC(currentLp.v.lp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T mapToObject(ObjectMapper mapper, JSONObject jsonObject, Class<T> valueType) {
        try {
            return mapper.readValue(jsonObject.toString(), valueType);
        } catch (JsonProcessingException e) {
        }
        return null;
    }

    /**
     * Scheduled job.
     */
    @Async("scheduledExecutor")
    @Scheduled(fixedDelay = 400)
    public void scheduledJob() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String sNow = dtf.format(now);
        log.trace("Scheduled task started: " + sNow);
        Set<FetcherRepoModel> tempDataQueue;
        synchronized (dataSet) {
            tempDataQueue = new HashSet<>(dataSet);
            dataSet.clear();
        }
        log.trace("Scheduled task populated: " + sNow);
        log.trace("Data size: " + tempDataQueue.size());
        repository.addToQueue(tempDataQueue, sNow);
        log.trace("Scheduled task ended: " + sNow);
        log.trace("");
    }

    /**
     * Create table.
     *
     * @param tableName the table name
     */
    public void createTable(String tableName) {
        try {
            repository.addTable(tableName);
        } catch (TickerException e) {
            throw e;
        } catch (Exception e) {
            throw new TickerException("Error while crating table: " + tableName);
        }
    }

    /**
     * Websocket handshake.
     *
     * @param thread    the thread
     * @param webSocket the web socket
     */
    public void handshake(FetcherThread thread, WebSocketClient webSocket) {
        if (thread.isEnabled()) {
            sendMessagesForInitializing(webSocket, thread);
            log.debug("{} : Handshake completed", thread.getThreadName());
        }
    }

    private void sendMessagesForInitializing(WebSocketClient webSocket, FetcherThread thread) {
        log.debug("{} : Creating websocket sessions", thread.getThreadName());
        sendMessage(webSocket, "{\"m\":\"set_auth_token\",\"p\":[\"unauthorized_user_token\"]}");
        sendMessage(webSocket, "{\"m\":\"set_locale\",\"p\":[\"en\",\"IN\"]}");
        sendMessage(webSocket, "{\"m\":\"chart_create_session\",\"p\":[\"" + thread.getChartSession() + "\",\"\"]}");
        sendMessage(webSocket, "{\"m\":\"quote_create_session\",\"p\":[\"" + thread.getQuoteSession() + "\"]}");
        waitFor(WAIT_QUICK);
        log.debug("{} : Adding symbols to websocket", thread.getThreadName());
        sendMessage(webSocket, "{\"m\":\"request_studies_metadata\",\"p\":[\"" + thread.getChartSession() + "\",\"metadata_1\"]}");
        sendMessage(webSocket, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSession() + "\",\"={\\\"adjustment\\\":\\\"splits\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\"}\"]}");
        sendMessage(webSocket, "{\"m\":\"quote_fast_symbols\",\"p\":[\"" + thread.getQuoteSession() + "\",\"={\\\"adjustment\\\":\\\"splits\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\"}\"]}");
        waitFor(WAIT_QUICK);
        log.debug("{} : Resolving symbol", thread.getThreadName());
        sendMessage(webSocket, "{\"m\":\"switch_timezone\",\"p\":[\"" + thread.getChartSession() + "\",\"Asia/Kolkata\"]}");
        sendMessage(webSocket, "{\"m\":\"resolve_symbol\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_sym_1\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
        waitFor(WAIT_QUICK);
        log.debug("{} : Creating series", thread.getThreadName());
        sendMessage(webSocket, "{\"m\":\"create_series\",\"p\":[\"" + thread.getChartSession() + "\",\"" + STUDY_SERIES_CODE + "\",\"s1\",\"sds_sym_1\",\"1\",50,\"\"]}");
        waitFor(WAIT_QUICK);
        log.debug("{} : Requesting more ticks", thread.getThreadName());
        sendMessage(webSocket, "{\"m\":\"request_more_tickmarks\",\"p\":[\"" + thread.getChartSession() + "\",\"" + STUDY_SERIES_CODE + "\",10]}");
        waitFor(WAIT_QUICK);
        log.debug("{} : Websocket initialization messages sent", thread.getThreadName());
    }

    /**
     * Update point value.
     *
     * @param thread the thread
     */
    public void updatePointValue(FetcherThread thread) {
        appService.updatePointValue(thread);
    }

    @Override
    protected Map<String, Executor> getExecutorMap() {
        Map<String, Executor> executorMap = new HashMap<>();
        executorMap.put("FetcherTaskExecutor", fetcherTaskExecutor);
        executorMap.put("ScheduledExecutor", scheduledExecutor);
        executorMap.put("RepoExecutor", repoExecutor);
        executorMap.put("ComputeEngine", ComputeEngine.getComputeExecutor());
        return executorMap;
    }

    @Scheduled(fixedDelay = 5000)
    public void cleanupWebsockets() {
        long start = System.currentTimeMillis();
        log.debug("cleanupWebsockets - Starting cleanup");
        List<WebSocketClient> webSocketClients;
        synchronized (activeWebsockets) {
            webSocketClients = new ArrayList<>(activeWebsockets);
        }

        webSocketClients.forEach(webSocketClient ->
                log.debug("{} :\t{} {} {}", webSocketClient, webSocketClient.isOpen(), webSocketClient.isClosing(), webSocketClient.isClosed()));

        Set<WebSocketClient> closedWebsockets = webSocketClients.stream()
                .filter(WebSocketClient::isClosed)
                .collect(Collectors.toSet());
        log.debug("{} closed websockets found", closedWebsockets.size());
        if (!closedWebsockets.isEmpty()) {
            log.debug("Removing {} websockets from the active list", closedWebsockets.size());
            synchronized (activeWebsockets) {
                activeWebsockets.removeAll(closedWebsockets);
            }
        }

        synchronized (websocketsToRemove) {
            closedWebsockets = websocketsToRemove.stream()
                    .filter(WebSocketClient::isClosed)
                    .collect(Collectors.toSet());
            log.debug("Removing {} websockets from the removal list", closedWebsockets.size());
            websocketsToRemove.removeAll(closedWebsockets);
        }
        synchronized (websocketsToRemove) {
            if (!websocketsToRemove.isEmpty()) {
                log.debug("Closing websockets that are to be removed");
                websocketsToRemove.forEach(webSocketClient -> {
                    try {
                        if (webSocketClient.isOpen()) {
                            log.debug("{} : Closing websocket", webSocketClient);
                            webSocketClient.close();
                        }
                    } catch (Exception e) {
                        log.error("{} : Error while closing websocket", webSocketClient, e);
                    }
                });
            }
        }

        log.debug("cleanupWebsockets - Completed in {}ms", System.currentTimeMillis() - start);
    }
}
