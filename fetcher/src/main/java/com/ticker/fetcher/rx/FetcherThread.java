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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ticker.common.contants.WebConstants.TRADING_VIEW_BASE;
import static com.ticker.common.contants.WebConstants.TRADING_VIEW_CHART;
import static com.ticker.common.util.Util.*;
import static com.ticker.fetcher.constants.FetcherConstants.FETCHER_THREAD_COMP_NAME;

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
    public static final int RETRY_LIMIT = 10;

    private final Object postInitLock = new Object();
    @Autowired
    private FetcherAppRepository repository;
    @Autowired
    private FetcherService fetcherService;
    private Set<String> fetcherApps = new HashSet<>();
    private float o;
    private float h;
    private float l;
    private float c;
    private float bbU;
    private float bbA;
    private float bbL;
    private float rsi;
    private float tema;
    private float currentValue;
    private long updatedAt;
    private boolean taskStarted = false;
    private String studySeries;
    private String studyBB;
    private String studyRSI;
    private String studyTEMA;

    private WebSocketClient webSocketClient;

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
    }

    public String getExchange() {
        return entity.getExchangeId();
    }

    public String getSymbol() {
        return entity.getSymbolId();
    }

    @Override
    protected void initialize() {
        try {
            initializeWebSocket();
        } catch (Exception e) {
            log.info(getThreadName() + " : Error while initializing websocket", e);
            throw new TickerException(getThreadName() + " : Error while initializing websocket");
        }
        initializeTables();
    }

    private void initializeWebSocket() throws IOException, URISyntaxException {
        FetcherThread thisThread = this;
        webSocketClient = new WebSocketClient(new URI("wss://data.tradingview.com/socket.io/websocket?from=chart%2F&date=" + getBuildTime())) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                log.info(getThreadName() + " : Opened websocket");
            }

            @Override
            public void onMessage(String message) {
                fetcherService.onReceiveMessage(thisThread, message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info(getThreadName() + " : Closed websocket, reason - " + reason);
            }

            @Override
            public void onError(Exception ex) {
                log.error(getThreadName() + " : Error in websocket", ex);
            }
        };
        webSocketClient.addHeader("Accept-Encoding", "gzip, deflate, br");
        webSocketClient.addHeader("Accept-Language", "en-IN,en;q=0.9");
        webSocketClient.addHeader("Cache-Control", "no-cache");
        webSocketClient.addHeader("Connection", "Upgrade");
        webSocketClient.addHeader("Host", "data.tradingview.com");
        webSocketClient.addHeader("Origin", "https://in.tradingview.com");
        webSocketClient.addHeader("Pragma", "no-cache");
        webSocketClient.addHeader("Sec-WebSocket-Extensions", "client_max_window_bits");
        webSocketClient.addHeader("Sec-WebSocket-Key", "rxPHgoX6myglC4x5XLaLtA==");
        webSocketClient.addHeader("Sec-WebSocket-Version", "13");
        webSocketClient.addHeader("Upgrade", "websocket");
        webSocketClient.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.80 Safari/537.36");
        webSocketClient.connect();
    }

    private String getBuildTime() throws IOException {
        URL url = new URL(TRADING_VIEW_BASE + TRADING_VIEW_CHART + getExchange() + ":" + getSymbol());
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
        return buildTime;
    }

    private void initializeTables() {
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
        initialize(0, false);
        while (isEnabled()) {
            while (isEnabled() && isInitialized()) {
                waitFor(WAIT_LONG);
            }
        }

        log.info("Terminated thread : " + getThreadName());
    }

    /**
     * Initialize.
     *
     * @param iteration the iteration
     * @param refresh   the refresh
     */
    protected void initialize(int iteration, boolean refresh) {
        this.initialized = false;
        if (refresh) {
            log.info(getExchange() + ":" + getSymbol() + " - Refreshing");
        } else {
            log.info(getExchange() + ":" + getSymbol() + " - Initializing");
        }

        try {

            waitFor(WAIT_LONG);
            if (refresh) {
//                getWebDriver().navigate().refresh();
            } else {
//                getWebDriver().get(url);
            }
//            fetcherService.setChartSettings(this, iteration, refresh);
            waitFor(WAIT_SHORT);
            log.debug(getThreadName() + " :" +
                    " getStudySeries(): " + getStudySeries() +
                    " getStudyBB(): " + getStudyBB() +
                    " getStudyRSI(): " + getStudyRSI() +
                    " getStudyTEMA(): " + getStudyTEMA());
            if (ObjectUtils.isEmpty(getStudySeries()) ||
                    ObjectUtils.isEmpty(getStudyBB()) ||
                    ObjectUtils.isEmpty(getStudyRSI()) ||
                    ObjectUtils.isEmpty(getStudyTEMA())) {
                throw new TickerException(getThreadName() + " : Error initializing study name");
            }
            setUpdatedAt(0);
            setInitialized(true);
        } catch (Exception e) {
            if (refresh) {
                log.warn("Error while refreshing " + getThreadName());
            } else {
                log.warn("Error while initializing " + getThreadName());
            }

            if (iteration < RETRY_LIMIT && isEnabled()) {
                initialize(iteration + 1, refresh);
            } else {
                if (refresh) {
                    log.error("Error while refreshing " + getThreadName(), e);
                } else {
                    log.error("Error while initializing " + getThreadName(), e);
                }
                log.error("Destorying " + getThreadName());
                destroy();
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

    @Deprecated
    public void destroy() {
        service.deleteTicker(this);
    }

    /**
     * Refresh browser.
     */
    public void refreshBrowser() {
        initialize(0, true);
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
            terminateThread();
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
    public float getCurrentValue() {
        return currentValue == 0 ? c : currentValue;
    }
}
