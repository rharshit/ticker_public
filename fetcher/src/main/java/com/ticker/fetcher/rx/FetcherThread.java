package com.ticker.fetcher.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.rx.TickerThread;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;
import static com.ticker.common.util.Util.*;
import static com.ticker.fetcher.constants.FetcherConstants.FETCHER_THREAD_COMP_NAME;
import static org.java_websocket.framing.CloseFrame.GOING_AWAY;
import static org.java_websocket.framing.CloseFrame.SERVICE_RESTART;

/**
 * The type Fetcher thread.
 */
@Getter
@Setter
@Slf4j
@Component(FETCHER_THREAD_COMP_NAME)
@Scope("prototype")
@NoArgsConstructor
public class FetcherThread extends TickerThread<TickerService> {

    /**
     * The constant RETRY_LIMIT.
     */
    public static final int RETRY_LIMIT = 3;

    private final Object postInitLock = new Object();
    @Autowired
    private FetcherAppRepository repository;
    @Autowired
    private FetcherService fetcherService;
    private Set<String> fetcherApps = new HashSet<>();
    private double o;
    private double h;
    private double l;
    private double c;
    private double bbU;
    private double bbA;
    private double bbL;
    private double rsi;
    private double tema;
    private double dayO;
    private double dayH;
    private double dayL;
    private double dayC;
    private double prevClose;
    private int pointValue;
    private double currentValue;
    private long updatedAt;
    private boolean taskStarted = false;
    private int requestId = 0;
    private String studySeries = "sds_1";
    private String studyBB = "st5";
    private String studyRSI = "st6";
    private String studyTEMA = "st7";

    private WebSocketClient webSocketClient;
    private static final Semaphore tempWebsocketFetcher;
    private String sessionId;
    private String clusterId;
    private String chartSession;
    private String quoteSession;
    private String quoteSessionTicker;
    private String quoteSessionTickerNew;
    private long lastPingAt = 0;

    private int retry = 0;
    private int incorrectValues = 0;

    private static final Semaphore websocketFetcher;

    static {
        websocketFetcher = new Semaphore(5);
        tempWebsocketFetcher = new Semaphore(250);
    }

    private static String buildTime = "";

    private Set<WebSocketClient> tempWebSocketClients = new HashSet<>();

    /**
     * Sets properties.
     *
     * @param apps the apps
     */
    public void setProperties(String... apps) {
        this.enabled = true;
        this.fetcherApps = Arrays.stream(apps).collect(Collectors.toSet());

        initialize();
    }

    public void setEntity(ExchangeSymbolEntity entity) {
        if (entity == null) {
            throw new TickerException("No entity found for the given exchange and symbol");
        }
        this.entity = entity;
        this.pointValue = entity.getLotSize() == null ? 0 : entity.getLotSize();
    }

    public String getExchange() {
        return entity.getExchangeId();
    }

    public String getSymbol() {
        return entity.getSymbolId();
    }

    @Override
    protected void initialize() {
        initializeTables();
    }

    private WebSocketClient initializeWebSocket(boolean temp) {
        long startTime = System.currentTimeMillis();
        WebSocketClient webSocket;
        Semaphore websocketSemaphore = temp ? tempWebsocketFetcher : websocketFetcher;
        try {
            while (!websocketSemaphore.tryAcquire()) {
                this.wait(WAIT_QUICK);
                if (System.currentTimeMillis() - startTime > 120000) {
                    throw new TickerException("Error while waiting to acquire websocket fetcher lock");
                }
            }
            if (!temp) {
                closeWebsocketIfExists(SERVICE_RESTART, "Restarting Websocket", webSocketClient);
            }
            FetcherThread thisThread = this;
            webSocket = new WebSocketClient(new URI("wss://data.tradingview.com/socket.io/websocket?from=chart%2F&date=" + getBuildTime())) {
                final long start = startTime;

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.debug(getThreadName() + " : Opened websocket");
                }

                @Override
                public void onMessage(String message) {
                    if (!temp) {
                        setLastPingAt(System.currentTimeMillis());
                    }
                    fetcherService.onReceiveMessage(thisThread, this, message, temp);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.debug(getThreadName() + " : Closed websocket, reason - " + reason);
                    if (!temp && isEnabled() && isInitialized()) {
                        refresh();
                    }
                    if (temp) {
                        log.debug(thisThread.getThreadName() + " : Closing temp websocket in " + (System.currentTimeMillis() - start) + "ms");
                    }
                }

                @Override
                public void onError(Exception ex) {
                    if (!temp) {
                        log.error(getThreadName() + " : Error in websocket", ex);
                    } else {
                        close(GOING_AWAY, "Error in temp websocket");
                    }
                }
            };
            webSocket.addHeader("Accept-Encoding", "gzip, deflate, br");
            webSocket.addHeader("Accept-Language", "en-IN,en;q=0.9");
            webSocket.addHeader("Cache-Control", "no-cache");
            webSocket.addHeader("Connection", "Upgrade");
            webSocket.addHeader("Host", "data.tradingview.com");
            webSocket.addHeader("Origin", "https://in.tradingview.com");
            webSocket.addHeader("Pragma", "no-cache");
            webSocket.addHeader("Sec-WebSocket-Extensions", "client_max_window_bits");
            webSocket.addHeader("Sec-WebSocket-Key", "rxPHgoX6myglC4x5XLaLtA==");
            webSocket.addHeader("Sec-WebSocket-Version", "13");
            webSocket.addHeader("Upgrade", "websocket");
            webSocket.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.80 Safari/537.36");
            webSocket.connect();

            createSession();

            setSessionId("");
            setRequestId(0);
            fetcherService.addSession(this, webSocket, temp);
            fetcherService.handshake(this, webSocket, temp);

            if (!temp) {
                setInitialized(true);
            }

        } catch (Exception e) {
            throw new TickerException(getThreadName() + " : Error while creating websocket");
        } finally {
            websocketSemaphore.release();
        }
        return webSocket;
    }

    private void createSession() {
        chartSession = "cs_" + createHash(12);
        quoteSession = "qs_" + createHash(12);
        quoteSessionTicker = "qs_" + createHash(12);
        quoteSessionTickerNew = "qs_" + createHash(12);
        log.debug(getThreadName() + " : chartSession - " + chartSession);
        log.debug(getThreadName() + " : quoteSession - " + quoteSession);
        log.debug(getThreadName() + " : quoteSessionTicker - " + quoteSessionTicker);
        log.debug(getThreadName() + " : quoteSessionTickerNew - " + quoteSessionTickerNew);
    }

    /**
     * Gets build time for charts.
     *
     * @return the build time
     */
    public static String getBuildTime() {
        synchronized (FetcherThread.buildTime) {
            if (FetcherThread.buildTime != null && !FetcherThread.buildTime.isEmpty()) {
                log.debug("Returning buildTime");
                return FetcherThread.buildTime;
            }
            try {
                log.info("Fetching and returning buildTime");
                URL url = new URL(TRADING_VIEW_BASE + TRADING_VIEW_CHART);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                String buildTime = "";
                while (StringUtils.isEmpty(buildTime) && (inputLine = in.readLine()) != null) {
                    try {
                        if (inputLine.contains("BUILD_TIME")) {
                            inputLine = inputLine.trim();
                            Pattern p = Pattern.compile("window\\.BUILD_TIME *= *\"(.*)\";");
                            Matcher m = p.matcher(inputLine);
                            if (m.matches()) {
                                buildTime = m.group(1);
                            }
                        }
                    } catch (Exception ignored) {

                    }
                }
                return (FetcherThread.buildTime = buildTime);
            } catch (Exception e) {

            }
            return null;
        }
    }

    /**
     * Initialize tables.
     */
    public void initializeTables() {
        String tableName = getTableName();
        fetcherService.createTable(tableName);
    }

    /**
     * Gets table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return this.entity.getFinalTableName();
    }

    @Override
    public void run() {
        initialize(false);
        while (isEnabled()) {
            while (isEnabled() && isInitialized()) {
                waitFor(WAIT_LONG);
                checkValues();
            }
        }
        terminateThread(false);
        log.info("Terminated thread : " + getThreadName());
    }

    private void checkValues() {
        if (isEnabled() && isInitialized()) {
            if (getCurrentValue() == 0 ||
                    getCurrentValue() > dayH ||
                    getCurrentValue() < dayL) {
                incorrectValues++;
                log.info(getThreadName() + " : incorrectValues " + incorrectValues + " - " + dayL + ", " + getCurrentValue() + ", " + dayH);
            }
        } else {
            incorrectValues = 0;
        }
        if (incorrectValues > 5) {
            refresh();
        }
    }

    /**
     * Initialize.
     *
     * @param refresh the refresh
     */
    protected void initialize(boolean refresh) {
        incorrectValues = 0;
        if (refresh) {
            log.info(getExchange() + ":" + getSymbol() + " - Refreshing");
        } else {
            setInitialized(false);
            log.info(getExchange() + ":" + getSymbol() + " - Initializing");
        }

        try {
            setLastPingAt(0);
            setUpdatedAt(0);
            webSocketClient = initializeWebSocket(false);
            log.debug(getThreadName() + " :" +
                    " getStudySeries(): " + getStudySeries() +
                    " getStudyBB(): " + getStudyBB() +
                    " getStudyRSI(): " + getStudyRSI() +
                    " getStudyTEMA(): " + getStudyTEMA());
            if (ObjectUtils.isEmpty(getStudySeries()) ||
                    ObjectUtils.isEmpty(getStudyBB()) ||
                    ObjectUtils.isEmpty(getStudyRSI()) ||
                    ObjectUtils.isEmpty(getStudyTEMA())) {
                log.error(getThreadName() + " :" +
                        " getStudySeries(): " + getStudySeries() +
                        " getStudyBB(): " + getStudyBB() +
                        " getStudyRSI(): " + getStudyRSI() +
                        " getStudyTEMA(): " + getStudyTEMA());
                throw new TickerException(getThreadName() + " : Error initializing study name");
            }
            if (refresh) {
                log.info(getExchange() + ":" + getSymbol() + " - Refreshed");
            } else {
                log.info(getExchange() + ":" + getSymbol() + " - Initialized");
            }
            retry = 0;
        } catch (Exception e) {
            setInitialized(false);
            if (refresh) {
                log.warn("Error while refreshing " + getThreadName());
            } else {
                log.warn("Error while initializing " + getThreadName());
            }
            log.info(getThreadName() + " : Retries - " + retry);
            if (retry < RETRY_LIMIT && isEnabled()) {
                retry++;
                initialize(refresh);
            } else {
                if (refresh) {
                    log.error("Error while refreshing " + getThreadName(), e);
                } else {
                    log.error("Error while initializing " + getThreadName(), e);
                }
                log.error("Destroying: " + getThreadName());
                service.deleteTicker(this);
            }
        }
    }

    @Override
    public String toString() {
        return "FetcherThread{" +
                "exchange='" + getExchange() + '\'' +
                ", symbol='" + getSymbol() + '\'' +
                '}';
    }

    /**
     * Refresh browser.
     */
    public void refresh() {
        initialize(true);
    }

    /**
     * Add app.
     *
     * @param appName the app name
     */
    public void addApp(String appName) {
        getFetcherApps().add(appName);
    }

    /**
     * Remove app.
     *
     * @param appName the app name
     */
    public void removeApp(String appName) {
        getFetcherApps().remove(appName);
        if (getFetcherApps().isEmpty()) {
            log.info("No apps fetching data");
            log.info("Terminating thread: " + getThreadName());
            terminateThread(false);
        }
    }

    @Override
    public void terminateThread(boolean shutDownInitiated) {
        super.terminateThread(shutDownInitiated);
        setInitialized(false);
        closeWebsocketIfExists(GOING_AWAY, "Terminating thread", webSocketClient);
        this.webSocketClient = null;
        removeTempWebSockets();
        service.deleteTicker(this);
    }

    private void closeWebsocketIfExists(int code, String reason, WebSocketClient webSocket) {
        if (webSocket != null) {
            log.debug(getThreadName() + " : Closing websocket");
            try {
                long start = System.currentTimeMillis();
                webSocket.close(code, reason);
                while (webSocket.isOpen() || webSocket.isClosing() || !webSocket.isClosed()) {
                    waitFor(WAIT_QUICK);
                    if (System.currentTimeMillis() - start >= 5000) {
                        break;
                    }
                }
            } catch (Exception ignored) {

            }
            tempWebSocketClients.remove(webSocket);
            log.debug(getThreadName() + " : Websocket closed");
        }
    }

    public String getThreadName() {
        return getTableName().replace(":", "_");
    }

    /**
     * Gets current value.
     *
     * @return the current value
     */
    public double getCurrentValue() {
        return currentValue == 0 ? c : currentValue;
    }

    private String createHash(int len) {
        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < len; i++) {
            hash.append(getAlphaNumericChar(getRandom().nextInt(62)));
        }
        return hash.toString();
    }

    /**
     * Gets alpha numeric char.
     *
     * @param x the x
     * @return the alpha numeric char
     */
    public char getAlphaNumericChar(int x) {
        int add = 0;
        if (x >= 0 && x <= 9) {
            add = '0';
        } else if (x >= 10 && x <= 35) {
            add = 'A' - 10;
        } else if (x >= 36 && x <= 61) {
            add = 'a' - 36;
        }
        return (char) (x + add);
    }

    /**
     * Sets point value.
     *
     * @param pointValue the point value
     */
    public void setPointValue(int pointValue) {
        if (this.pointValue != pointValue) {
            this.pointValue = pointValue;
            if (pointValue > 1) {
                fetcherService.updatePointValue(this);
            }
        }
    }

    /**
     * Sets session id.
     *
     * @param sessionId the session id
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        Pattern p = Pattern.compile("window\\.BUILD_TIME *= *\"(.*)\";");
        Matcher m = p.matcher(sessionId);
        this.clusterId = m.matches() ? m.group(2) : "";
    }

    public void addTempWebSocket() {
        WebSocketClient webSocket = initializeWebSocket(true);
        tempWebSocketClients.add(webSocket);
    }

    public void removeTempWebSockets() {
        WebSocketClient[] tempWebSocketArr = tempWebSocketClients.toArray(new WebSocketClient[0]);
        for (WebSocketClient webSocketClient : tempWebSocketArr) {
            closeWebsocketIfExists(GOING_AWAY, "Terminating temp websocket", webSocketClient);
        }
    }
}
