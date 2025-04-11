package com.ticker.fetcher.rx;

import com.ticker.common.entity.exchangesymbol.ExchangeSymbolEntity;
import com.ticker.common.exception.TickerException;
import com.ticker.common.rx.TickerThread;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.service.FetcherService;
import com.ticker.fetcher.service.TickerService;
import com.ticker.fetcher.utils.TimeUtil;
import com.ticker.fetcher.utils.compute.ComputeEngine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;
import static com.ticker.common.util.Util.*;
import static com.ticker.fetcher.constants.FetcherConstants.FETCHER_THREAD_COMP_NAME;
import static com.ticker.fetcher.utils.TimeUtil.MINUTE_IN_MILLI;
import static com.ticker.fetcher.utils.TimeUtil.SECOND_IN_MILLI;

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
    private static String buildTime = "";
    public static final String STUDY_SERIES_CODE = "sds_1";
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
    private boolean prevDataPopulated = false;
    private int requestId = 0;
    private WebSocketClient webSocketClient;
    private static final Semaphore websocketInitializeSemaphore = new Semaphore(5);
    private final Object websocketLock = new Object();
    private final Object websocketInitializeLock = new Object();
    private final Set<WebSocketClient> previousWebsockets = new HashSet<>();
    private boolean isRequestWebsocketInitialize = false;
    private long websocketInitializeTime = 0;

    private String sessionId;
    private String clusterId;
    private String chartSession;
    private String quoteSession;
    private long lastPingAt = 0;
    private int retry = 0;
    private int incorrectValues = 0;
    private Executor refreshExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private long lastDailyValueUpdatedAt = System.currentTimeMillis();

    private final ComputeEngine computeEngine = new ComputeEngine(this);
    private final ScheduledExecutorService cutoffScheduler = Executors.newScheduledThreadPool(2);
    private long currentMinute;
    private long currentSecond;

    private void requestWebsocketInitialize() {
        synchronized (websocketInitializeLock) {
            isRequestWebsocketInitialize = true;
        }
    }

    /**
     * Gets build time for charts.
     *
     * @return the build time
     */
    public static String fetchBuildTime() {
        synchronized (FetcherThread.buildTime) {
            if (FetcherThread.buildTime != null && !FetcherThread.buildTime.isEmpty()) {
                log.debug("Returning buildTime");
                return FetcherThread.buildTime;
            }
            try {
                log.info("Fetching buildTime");
                URL url = new URL(TRADING_VIEW_BASE + TRADING_VIEW_CHART);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                String buildTime = "";
                while (!StringUtils.hasText(buildTime) && (inputLine = in.readLine()) != null) {
                    try {
                        if (inputLine.contains("BUILD_TIME")) {
                            inputLine = inputLine.trim();
                            Pattern p = Pattern.compile("window\\.BUILD_TIME *= *\"(.*)\";");
                            Matcher m = p.matcher(inputLine);
                            if (m.matches()) {
                                buildTime = m.group(1);
                                FetcherThread.buildTime = buildTime;
                                log.info("Build time : {}", buildTime);
                                return buildTime;
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

    public void setC(double c) {
        this.c = c;
        computeEngine.updateLastPrice(c, System.currentTimeMillis());
    }

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
        initializeCutoffs();
    }

    private void initializeCutoffs() {
        long currentTimestamp = System.currentTimeMillis();
        cutoffScheduler.scheduleAtFixedRate(() -> {
                    if (isInitialized()) {
                        FetcherService.addCurrentValueToDataset(this);
                    }
                },
                TimeUtil.timeToNextSecond(currentTimestamp), SECOND_IN_MILLI, TimeUnit.MILLISECONDS);
        cutoffScheduler.scheduleAtFixedRate(this::minuteCutoff,
                TimeUtil.timeToNextMinute(currentTimestamp), MINUTE_IN_MILLI, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(cutoffScheduler::shutdown));
    }

    private void minuteCutoff() {
        log.trace("{} : minuteCutoff initiated at {}", getThreadName(), System.currentTimeMillis());
        computeEngine.minuteCutoff();
        log.trace("{} : minuteCutoff completed at {}", getThreadName(), System.currentTimeMillis());
    }

    public void addPrevBarData(List<ComputeEngine.ComputeData> values) {
        log.debug("{} : Populating previous data", getThreadName());
        computeEngine.addValues(values);
        prevDataPopulated = true;
    }

    private void initializeNewWebSocket(boolean refresh) {
        requestWebsocketInitialize();
        long startTime = System.currentTimeMillis();
        synchronized (websocketInitializeLock) {
            if (!refresh || isRequestWebsocketInitialize || startTime > websocketInitializeTime) {
                log.info("{} - {} websocket : {} {} {} {} {} {}", getThreadName(), refresh ? "Refreshing" : "Initializing new",
                        refresh, isRequestWebsocketInitialize, startTime > websocketInitializeTime, startTime, websocketInitializeTime, startTime - websocketInitializeTime);
                boolean initialized = false;
                WebSocketClient webSocket = null;
                try {
                    FetcherThread thisThread = this;
                    boolean initializeAllowed = websocketInitializeSemaphore.tryAcquire();
                    while (!initializeAllowed) {
                        log.trace("{} : Waiting to acquire semaphore lock to connect websocket", getThreadName());
                        waitFor(WAIT_SHORT);
                        initializeAllowed = websocketInitializeSemaphore.tryAcquire();
                    }
                    log.debug("{} : Semaphore lock to connect websocket acquired", getThreadName());
                    try {
                        webSocket = new WebSocketClient(new URI("wss://data.tradingview.com/socket.io/websocket?from=chart%2F&date=" + fetchBuildTime() + "&type=chart")) {
                            final long start = startTime;

                            @Override
                            public void onOpen(ServerHandshake handshakedata) {
                                setLastPingAt(System.currentTimeMillis());
                                log.debug("{} : Opened websocket", getThreadName());
                            }

                            @Override
                            public void onMessage(String message) {
                                setLastPingAt(System.currentTimeMillis());
                                fetcherService.onReceiveMessage(thisThread, this, message);
                            }

                            @Override
                            public void onClose(int code, String reason, boolean remote) {
                                log.debug("{} - {} : Closing websocket {}", getThreadName(), reason, this);
                            }

                            @Override
                            public void onError(Exception ex) {
                                log.error("{} : Error in websocket", getThreadName(), ex);
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
                        webSocket.addHeader("Sec-WebSocket-Key", createHash(22) + "==");
                        webSocket.addHeader("Sec-WebSocket-Version", "13");
                        webSocket.addHeader("Upgrade", "websocket");
                        webSocket.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.80 Safari/537.36");
                        webSocket.connect();

                        FetcherService.addToActiveWebsockets(webSocket);

                        createSession();
                        setSessionId("");
                        setRequestId(0);

                        log.debug("{} : Waiting for websocket to open", getThreadName());
                        while (isEnabled() && !webSocket.isOpen()) {
                            if (refresh && System.currentTimeMillis() - startTime > 10000) {
                                throw new TickerException(getThreadName() + " : Timeout while waiting for websocket to open");
                            }
                            waitFor(WAIT_SHORT);
                        }
                        log.debug("{} : Websocket opened in {}ms", getThreadName(), System.currentTimeMillis() - startTime);

                        fetcherService.handshake(this, webSocket);
                    } catch (Exception e) {
                        log.error("{} : Error while initializing new websocket", getThreadName(), e);
                    } finally {
                        websocketInitializeSemaphore.release();
                        log.debug("{} : Semaphore lock to connect websocket released", getThreadName());
                    }

                    replaceWebsocket(webSocket);
                    setInitialized(true);
                    initialized = true;
                    log.info("{} : Websocket {}initialized in {}ms", getThreadName(), refresh ? "re-" : "", System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("{} - Error while creating websocket", getThreadName(), e);
                    if (webSocket != null) {
                        FetcherService.closeWebSocket(webSocket);
                    }
                }
                if (!initialized && !refresh) {
                    log.info("{} - Re-initializing websocket", getThreadName());
                    initializeNewWebSocket(false);
                }
            } else {
                log.info("{} : Websocket already initialized", getThreadName());
            }
        }
    }

    private void replaceWebsocket(WebSocketClient webSocket) {
        synchronized (previousWebsockets) {
            previousWebsockets.add(this.webSocketClient);
            log.debug("{} : Moving to previous websockets {}", getThreadName(), this.webSocketClient);
        }
        synchronized (websocketLock) {
            this.webSocketClient = webSocket;
            log.debug("{} : New websocket {}", getThreadName(), webSocket);
        }
        websocketInitializeTime = System.currentTimeMillis();
    }

    private void createSession() {
        chartSession = "cs_" + createHash(12);
        quoteSession = "qs_" + createHash(12);
        log.debug("{} : chartSession - {}", getThreadName(), chartSession);
        log.debug("{} : quoteSession - {}", getThreadName(), quoteSession);
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
        initializeTicker();
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
        long now = System.currentTimeMillis();
        if (isEnabled() && isInitialized()) {
            if (isIncorrectValue()) {
                incorrectValues++;
                log.debug(getThreadName() + " : incorrectValues " + incorrectValues + " - " + dayL + ", " + getCurrentValue() + ", " + dayH + ", " + prevDataPopulated);
            } else {
                if (incorrectValues != 0) {
                    log.info(getThreadName() + " : Values corrected - " + dayL + ", " + getCurrentValue() + ", " + dayH + ", " + prevDataPopulated);
                }
                incorrectValues = 0;
                if (now - lastDailyValueUpdatedAt > 180000 && now - updatedAt < 30000) {
                    log.info("{} - Refreshing daily values: {} {}", getThreadName(), now - lastDailyValueUpdatedAt, now - updatedAt);
                    refresh();
                }
            }
        } else {
            incorrectValues = 0;
            log.debug("{} : Not initialized", getThreadName());
        }
        if (incorrectValues > 5) {
            log.info("{} : Incorrect values {} - {}, {}, {}, {}", getThreadName(), incorrectValues, dayL, getCurrentValue(), dayH, prevDataPopulated);
            refresh();
            incorrectValues = 1;
        }
    }

    private boolean isIncorrectValue() {
        return getCurrentValue() == 0 ||
                getCurrentValue() > dayH ||
                getCurrentValue() < dayL ||
                !prevDataPopulated;
    }

    /**
     * Initialize websocket, and update the metadata for the ticker
     *
     */
    protected void initializeTicker() {
        incorrectValues = 0;
        setInitialized(false);
        log.info(getExchange() + ":" + getSymbol() + " - Initializing");

        try {
            setLastPingAt(0);
            setUpdatedAt(0);
            initializeNewWebSocket(false);
            log.info(getExchange() + ":" + getSymbol() + " - Initialized");
            retry = 0;
        } catch (Exception e) {
            setInitialized(false);
            log.warn("{} - Error while initializing", getThreadName());
            log.info("{} : Retries - {}", getThreadName(), retry);
            if (retry < RETRY_LIMIT && isEnabled()) {
                retry++;
                initializeTicker();
            } else {
                log.error("{} - Error while initializing", getThreadName(), e);
                log.error("{} - Destroying", getThreadName());
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
        refreshExecutor.execute(() -> initializeNewWebSocket(true));
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
        replaceWebsocket(null);
        super.terminateThread(shutDownInitiated);
        setInitialized(false);
        setEnabled(false);
        closePreviousWebsockets();
        service.deleteTicker(this);
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
     * Gets alphanumeric char.
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

    public void closePreviousWebsockets() {
        log.trace("{} : Closing previous websockets", getThreadName());
        List<WebSocketClient> websocketsToClose;
        synchronized (previousWebsockets) {
            websocketsToClose = new ArrayList<>(previousWebsockets);
            previousWebsockets.clear();
        }
        FetcherService.closeWebSockets(websocketsToClose.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }
}
