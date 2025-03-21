package com.ticker.fetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.service.BaseService;
import com.ticker.fetcher.model.FetcherRepoModel;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.rx.FetcherThread;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import static com.ticker.common.util.Util.WAIT_QUICK;
import static com.ticker.common.util.Util.waitFor;
import static com.ticker.fetcher.FetcherUtil.decodeMessage;
import static com.ticker.fetcher.FetcherUtil.encodeMessage;

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

    /**
     * Send message.
     *
     * @param thread the thread
     * @param data   the data
     */
    public void sendMessage(FetcherThread thread, String data) {
        if (thread.getWebSocketClient() != null && thread.getWebSocketClient().isOpen()) {
            log.debug(thread.getThreadName() + " : sending message\n" + data);
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
        if (webSocket != null && webSocket.isOpen()) {
            log.trace("Sending message:");
            log.trace(data);
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
     * @param temp
     */
    public void onReceiveMessage(FetcherThread thread, WebSocketClient webSocket, String data, boolean temp) {
        log.trace("Recv:");
        String[] messages = decodeMessage(data);
        for (String message : messages) {
            if (Pattern.matches("~h~\\d*$", message)) {
                if (!temp) {
                    sendMessage(webSocket, message);
                }
            } else {
                parseMessage(thread, message, temp);
            }
        }
    }

    private void parseMessage(FetcherThread thread, String message, boolean temp) {
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
                        if (hasInterestedValue(thread, jsonObject)) {
                            thread.getExecutor().execute(() -> setVal(thread, jsonObject, temp));
                        }
                    } catch (Exception ignored) {

                    }
                }
            }
        } catch (Exception ignore) {

        }
    }

    private boolean hasInterestedValue(FetcherThread thread, JSONObject jsonObject) {
        if (jsonObject.has(thread.getStudySeries())) {
            return true;
        }
        if (jsonObject.has("v")) {
            JSONObject value = jsonObject.getJSONObject("v");
            return value.has("lp") ||
                    value.has("open_price") ||
                    value.has("high_price") ||
                    value.has("low_price");
        }
        return false;
    }

    private void setVal(FetcherThread thread, JSONObject object, boolean temp) {
        log.debug(thread.getThreadName() + " : Setting value");
        log.debug(object.toString(2));
        for (String key : object.keySet()) {
            log.trace("Key: " + key);
            try {
                Double[] vals = null;
                try {
                    vals = getVals(object.getJSONObject(key).getJSONArray("st"));
                } catch (Exception ignored) {
                }
                if (vals == null) {
                    try {
                        vals = getVals(object.getJSONObject(key).getJSONArray("s"));
                    } catch (Exception ignored) {
                    }
                }
                if (thread.getStudySeries().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting OHLC value");
                    thread.setO(vals[1]);
                    thread.setH(vals[2]);
                    thread.setL(vals[3]);
                    thread.setC(vals[4]);
                } else if ("v".equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting day values value");
                    JSONObject value = object.getJSONObject("v");
                    if (value.has("open_price")) {
                        thread.setDayO(value.getDouble("open_price"));
                    }
                    if (value.has("high_price")) {
                        thread.setDayH(value.getDouble("high_price"));
                    }
                    if (value.has("low_price")) {
                        thread.setDayL(value.getDouble("low_price"));
                    }
                    if (value.has("lp")) {
                        thread.setDayC(value.getDouble("lp"));
                    }
                    if (value.has("prev_close_price")) {
                        thread.setPrevClose(value.getDouble("prev_close_price"));
                    }
                    if (value.has("pointvalue")) {
                        thread.setPointValue(value.getInt("pointvalue"));
                    }
                    if (temp
                            && value.has("open_price")
                            && value.has("high_price")
                            && value.has("low_price")
                            && value.has("lp")
                            && value.has("prev_close_price")) {
                        thread.setLastDailyValueUpdatedAt(System.currentTimeMillis());
                        thread.finishTempWebsocketTask();
                    }
                }
                if (!temp) {
                    thread.setUpdatedAt(System.currentTimeMillis());
                }
                synchronized (dataSet) {
                    dataSet.add(new FetcherRepoModel(thread));
                }
            } catch (Exception ignored) {

            }
        }
    }


    private Double[] getVals(JSONArray arr) {
        int maxIndex = 0;
        double maxTime = 0;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject object = arr.getJSONObject(i);
                JSONArray vals = object.getJSONArray("v");
                double time = vals.getDouble(0);
                if (time > maxTime) {
                    maxIndex = i;
                    maxTime = time;
                }
            } catch (Exception ignored) {

            }
        }
        JSONObject object = arr.getJSONObject(maxIndex);
        JSONArray vals = object.getJSONArray("v");
        return vals.toList().stream().map(o -> {
            if (o instanceof Double) {
                return (Double) o;
            } else if (o instanceof Integer) {
                return Double.valueOf((Integer) o);
            }
            return 0.0;
        }).toArray(Double[]::new);
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
     * @param temp
     */
    public void handshake(FetcherThread thread, WebSocketClient webSocket, boolean temp) {
        if (webSocket == null) {
            throw new TickerException(thread.getThreadName() + " : Null websocket");
        }
        long startTime = System.currentTimeMillis();
        while (thread.isEnabled() && !webSocket.isOpen()) {
            if (System.currentTimeMillis() - startTime > 10000 && !temp) {
                throw new TickerException(thread.getThreadName() + " : Timeout while waiting for websocket to open");
            }
            waitFor(WAIT_QUICK);
        }
        if (thread.isEnabled()) {
            sendMessagesForInitializing(webSocket, thread);
            log.debug("Sent messages");

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
        sendMessage(webSocket, "{\"m\":\"create_series\",\"p\":[\"" + thread.getChartSession() + "\",\"" + thread.getStudySeries() + "\",\"s1\",\"sds_sym_1\",\"1\",300,\"\"]}");
        waitFor(WAIT_QUICK);
        log.debug("{} : Requesting more ticks", thread.getThreadName());
        sendMessage(webSocket, "{\"m\":\"request_more_tickmarks\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_1\",10]}");
        waitFor(WAIT_QUICK);
        log.debug("{} : Websocket initialization messages sent", thread.getThreadName());
    }

    /**
     * Add session to the thread.
     *
     * @param thread    the thread
     * @param webSocket the web socket
     * @param temp
     */
    public void addSession(FetcherThread thread, WebSocketClient webSocket, boolean temp) {
        if (webSocket == null) {
            throw new TickerException(thread.getThreadName() + " : Null websocket");
        }
        long startTime = System.currentTimeMillis();
        while (thread.isEnabled() && !webSocket.isOpen()) {
            if (System.currentTimeMillis() - startTime > 10000 && !temp) {
                throw new TickerException(thread.getThreadName() + " : Timeout while waiting for websocket to open");
            }
            waitFor(WAIT_QUICK);
        }
        startTime = System.currentTimeMillis();
        while (ObjectUtils.isEmpty(thread.getSessionId())) {
            if (System.currentTimeMillis() - startTime > 10000 && !temp) {
                throw new TickerException(thread.getThreadName() + " : Timeout while waiting for Session ID");
            }
            waitFor(WAIT_QUICK);
        }
        log.debug(thread.getThreadName() + " : Session set - " + thread.getSessionId());
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
        return executorMap;
    }
}
