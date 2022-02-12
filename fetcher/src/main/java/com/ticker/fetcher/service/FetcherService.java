package com.ticker.fetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.service.BaseService;
import com.ticker.fetcher.model.FetcherRepoModel;
import com.ticker.fetcher.repository.FetcherAppRepository;
import com.ticker.fetcher.rx.FetcherThread;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

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

    private static final List<FetcherRepoModel> dataQueue = new ArrayList<>();
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
     * Gets executor details.
     *
     * @return the executor details
     */
    public Map<String, Map<String, Integer>> getExecutorDetails() {
        Map<String, Map<String, Integer>> details = new HashMap<>();
        details.put("fetcherTaskExecutor", getExecutorDetails(fetcherTaskExecutor));
        details.put("scheduledExecutor", getExecutorDetails(scheduledExecutor));
        details.put("repoExecutor", getExecutorDetails(repoExecutor));
        return details;
    }

    public void sendMessage(FetcherThread thread, String data) {
        log.trace(thread.getThreadName() + " : sending message\n" + data);
        thread.getWebSocketClient().send(encodeMessage(data));
    }

    /**
     * On message received.
     *
     * @param thread the thread
     * @param data   the data
     */
    public void onReceiveMessage(FetcherThread thread, String data) {
        log.info("Recv:");
        fetcherTaskExecutor.execute(() -> {
            String[] messages = decodeMessage(data);
            for (String message : messages) {
                log.info("\n" + message);
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
                                if (jsonObject.has(thread.getStudySeries())
                                        || jsonObject.has(thread.getStudyBB())
                                        || jsonObject.has(thread.getStudyRSI())
                                        || jsonObject.has(thread.getStudyTEMA())) {
                                    setVal(thread, jsonObject);
                                }
                            } catch (Exception ignored) {

                            }
                        }
                    }
                } catch (Exception ignored) {

                }
            }
        });
    }

    private void setVal(FetcherThread thread, JSONObject object) {
        log.trace(thread.getThreadName() + " : Setting value");
        for (String key : object.keySet()) {
            log.trace("Key: " + key);
            try {
                Float[] vals;
                try {
                    vals = getVals(object.getJSONObject(key).getJSONArray("st"));

                } catch (Exception e) {
                    vals = getVals(object.getJSONObject(key).getJSONArray("s"));

                }
                if (thread.getStudySeries().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting OHLC value");
                    thread.setO(vals[1]);
                    thread.setH(vals[2]);
                    thread.setL(vals[3]);
                    thread.setC(vals[4]);
                } else if (thread.getStudyBB().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting BB value");
                    thread.setBbA(vals[1]);
                    thread.setBbU(vals[2]);
                    thread.setBbL(vals[3]);
                } else if (thread.getStudyRSI().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting RSI value");
                    thread.setRsi(vals[1]);
                } else if (thread.getStudyTEMA().equals(key)) {
                    log.trace(thread.getThreadName() + " : Setting TEMA value");
                    thread.setTema(vals[1]);
                }
                thread.setUpdatedAt((long) ((float) vals[0]));
                synchronized (dataQueue) {
                    dataQueue.add(new FetcherRepoModel(thread));
                }
            } catch (Exception ignored) {

            }
        }
    }


    private Float[] getVals(JSONArray arr) {
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
        return vals.toList().stream().map(o -> Float.parseFloat(o.toString())).toArray(Float[]::new);
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
        List<FetcherRepoModel> tempDataQueue;
        synchronized (dataQueue) {
            tempDataQueue = new ArrayList<>(dataQueue);
            dataQueue.clear();
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

    public void handshake(FetcherThread thread) {
        while (thread.isEnabled() && !thread.getWebSocketClient().isOpen()) {
            waitFor(WAIT_QUICK);
        }
        if (thread.isEnabled()) {
            sendMessage(thread, "{\"m\":\"set_auth_token\",\"p\":[\"unauthorized_user_token\"]}");
            sendMessage(thread, "{\"m\":\"chart_create_session\",\"p\":[\"" + thread.getChartSession() + "\",\"\"]}");
            sendMessage(thread, "{\"m\":\"quote_create_session\",\"p\":[\"" + thread.getQuoteSession() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_set_fields\",\"p\":[\"" + thread.getQuoteSession() + "\",\"base-currency-logoid\",\"ch\",\"chp\",\"currency-logoid\",\"currency_code\",\"current_session\",\"description\",\"exchange\",\"format\",\"fractional\",\"is_tradable\",\"language\",\"local_description\",\"logoid\",\"lp\",\"lp_time\",\"minmov\",\"minmove2\",\"original_name\",\"pricescale\",\"pro_name\",\"short_name\",\"type\",\"update_mode\",\"volume\",\"rchp\",\"rtc\",\"country_code\",\"provider_id\"]}");
            sendMessage(thread, "{\"m\":\"request_studies_metadata\",\"p\":[\"" + thread.getChartSession() + "\",\"metadata_1\"]}");
            sendMessage(thread, "{\"m\":\"resolve_symbol\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_sym_1\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"create_series\",\"p\":[\"" + thread.getChartSession() + "\",\"" + thread.getStudySeries() + "\",\"s1\",\"sds_sym_1\",\"D\",300,\"\"]}");
            sendMessage(thread, "{\"m\":\"switch_timezone\",\"p\":[\"" + thread.getChartSession() + "\",\"Asia/Kolkata\"]}");
            sendMessage(thread, "{\"m\":\"quote_create_session\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"create_study\",\"p\":[\"" + thread.getChartSession() + "\",\"st1\",\"st1\",\"" + thread.getStudySeries() + "\",\"Dividends@tv-basicstudies-149\",{}]}");
            sendMessage(thread, "{\"m\":\"create_study\",\"p\":[\"" + thread.getChartSession() + "\",\"st2\",\"st1\",\"" + thread.getStudySeries() + "\",\"Splits@tv-basicstudies-149\",{}]}");
            sendMessage(thread, "{\"m\":\"create_study\",\"p\":[\"" + thread.getChartSession() + "\",\"st3\",\"st1\",\"" + thread.getStudySeries() + "\",\"Earnings@tv-basicstudies-149\",{}]}");
            sendMessage(thread, "{\"m\":\"quote_create_session\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\",\"" + thread.getExchange() + ":" + thread.getSymbol() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_set_fields\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\",\"base-currency-logoid\",\"ch\",\"chp\",\"currency-logoid\",\"currency_code\",\"current_session\",\"description\",\"exchange\",\"format\",\"fractional\",\"is_tradable\",\"language\",\"local_description\",\"logoid\",\"lp\",\"lp_time\",\"minmov\",\"minmove2\",\"original_name\",\"pricescale\",\"pro_name\",\"short_name\",\"type\",\"update_mode\",\"volume\"]}");

            log.info("Sent messages");

        }
    }

    public void addSession(FetcherThread thread) {
        while (thread.isEnabled() && !thread.getWebSocketClient().isOpen()) {
            waitFor(WAIT_QUICK);
        }
        while (ObjectUtils.isEmpty(thread.getSessionId())) {
            waitFor(WAIT_QUICK);
        }
        log.info(thread.getThreadName() + " : Session set - " + thread.getSessionId());
    }
}
