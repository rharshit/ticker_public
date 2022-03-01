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


    static {
        FetcherThread.getBuildTime();
    }

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
        if (thread.getWebSocketClient().isOpen()) {
            log.debug(thread.getThreadName() + " : sending message\n" + data);
            thread.getWebSocketClient().send(encodeMessage(data));
            waitFor(5);
        } else {
            log.warn(thread.getThreadName() + " : Cannot send message, websocket not open");
        }
    }

    /**
     * On message received.
     *
     * @param thread the thread
     * @param data   the data
     */
    public void onReceiveMessage(FetcherThread thread, String data) {
        log.debug("Recv:");
        fetcherTaskExecutor.execute(() -> {
            String[] messages = decodeMessage(data);
            for (String message : messages) {
                String parsedMessage = message;
                try {
                    JSONObject jsonObject = new JSONObject(message);
                    parsedMessage = jsonObject.toString(2);
                } catch (Exception e) {

                }
                log.debug("\n" + parsedMessage);
                if (Pattern.matches("~h~\\d*$", message)) {
                    sendMessage(thread, message);
                } else {
                    fetcherTaskExecutor.execute(() -> parseMessage(thread, message));
                }
            }
        });
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
                        if (hasInterestedValue(thread, jsonObject)) {
                            setVal(thread, jsonObject);
                        }
                    } catch (Exception ignored) {

                    }
                }
            }
        } catch (Exception ignore) {

        }
    }

    private boolean hasInterestedValue(FetcherThread thread, JSONObject jsonObject) {
        if (jsonObject.has(thread.getStudySeries())
                || jsonObject.has(thread.getStudyBB())
                || jsonObject.has(thread.getStudyRSI())
                || jsonObject.has(thread.getStudyTEMA())) {
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

    private void setVal(FetcherThread thread, JSONObject object) {
        log.trace(thread.getThreadName() + " : Setting value");
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
                }
                thread.setUpdatedAt((long) (vals[0] * 1000));
                synchronized (dataQueue) {
                    dataQueue.add(new FetcherRepoModel(thread));
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
            // Add symbol and fetch chart data
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
            sendMessage(thread, "{\"m\":\"quote_set_fields\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\",\"base-currency-logoid\",\"ch\",\"chp\",\"currency-logoid\",\"currency_code\",\"current_session\",\"description\",\"exchange\",\"format\",\"fractional\",\"is_tradable\",\"language\",\"local_description\",\"logoid\",\"lp\",\"lp_time\",\"minmov\",\"minmove2\",\"original_name\",\"pricescale\",\"pro_name\",\"short_name\",\"type\",\"update_mode\",\"volume\"]}");
            sendMessage(thread, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\",\"" + thread.getExchange() + ":" + thread.getSymbol() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_fast_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"quote_remove_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"quote_fast_symbols\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\",\"" + thread.getExchange() + ":" + thread.getSymbol() + "\"]}");
            sendMessage(thread, "{\"m\":\"request_more_tickmarks\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_1\",10]}");
            sendMessage(thread, "{\"m\":\"create_study\",\"p\":[\"" + thread.getChartSession() + "\",\"st4\",\"st1\",\"sds_1\",\"Volume@tv-basicstudies-149\",{\"length\":20,\"col_prev_close\":false}]}");
            sendMessage(thread, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"" + thread.getExchange() + ":" + thread.getSymbol() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_remove_symbols\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\",\"" + thread.getExchange() + ":" + thread.getSymbol() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_fast_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\"}\",\"" + thread.getExchange() + ":" + thread.getSymbol() + "\"]}");
            sendMessage(thread, "{\"m\":\"quote_hibernate_all\",\"p\":[\"" + thread.getQuoteSessionTickerNew() + "\"]}");
            waitFor(WAIT_QUICK);

            // Modify chart
            sendMessage(thread, "{\"m\":\"resolve_symbol\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_sym_2\",\"={\\\"symbol\\\":{\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\"},\\\"type\\\":\\\"BarSetHeikenAshi@tv-basicstudies-60!\\\",\\\"inputs\\\":{}}\"]}");
            sendMessage(thread, "{\"m\":\"modify_series\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_1\",\"s2\",\"sds_sym_2\",\"D\",\"\"]}");
            sendMessage(thread, "{\"m\":\"quote_remove_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\",\\\"session\\\":\\\"regular\\\"}\"]}");
            waitFor(WAIT_QUICK);
            sendMessage(thread, "{\"m\":\"resolve_symbol\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_sym_3\",\"={\\\"symbol\\\":{\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\",\\\"session\\\":\\\"regular\\\"},\\\"type\\\":\\\"BarSetHeikenAshi@tv-basicstudies-60!\\\",\\\"inputs\\\":{}}\"]}");
            sendMessage(thread, "{\"m\":\"modify_series\",\"p\":[\"" + thread.getChartSession() + "\",\"sds_1\",\"s3\",\"sds_sym_3\",\"1\",\"\"]}");
            sendMessage(thread, "{\"m\":\"quote_remove_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"quote_add_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\",\\\"session\\\":\\\"regular\\\"}\"]}");
            sendMessage(thread, "{\"m\":\"quote_fast_symbols\",\"p\":[\"" + thread.getQuoteSessionTicker() + "\",\"" + thread.getExchange() + ":" + thread.getSymbol() + "\",\"={\\\"symbol\\\":\\\"" + thread.getExchange() + ":" + thread.getSymbol() + "\\\",\\\"currency-id\\\":\\\"INR\\\",\\\"adjustment\\\":\\\"splits\\\",\\\"session\\\":\\\"regular\\\"}\"]}");
            waitFor(WAIT_QUICK);

            // Add studies
            sendMessage(thread, "{\"m\":\"create_study\",\"p\":[\"" + thread.getChartSession() + "\",\"" + thread.getStudyBB() + "\",\"st1\",\"sds_1\",\"Script@tv-scripting-101!\",{\"text\":\"zW1+wVOc52/yPnU4LWhyTw==_htNzUrc457Lyx9myW31mZZKXTme5DvaQUPCkw82+J6qvgURkPjRtam7d09ED0nuuEjGHKncBMo2MkN/YUUk1DUZAfFeYIbzm4UdSxVKs6nXa/Wc181Ai+nThySLOr7mi6sdkdSPxpg3yx1kWeTsNORKWjXvcJszgeMjQq69LtbzV35Qg+fIi+4MI8jzVoGn1KeQGltl0pBVvZmdC8zL+W1RGCtfNz8IFIHo2i2OjQlX5zza2JQNZVBDQ6EFeAVmIm0HlK63Q31NsbT5pFPMCSAmK9TQ5c/5fkiBNnQP6UjYAs5j6NwTSr7RSx9un5viiyXa0EmFlYteP2lDJ8zXSKnmxPt+H7CtPKHp0PyYI8s1ySA3wR5Kt5oKK+ggNheJNW40tS3Y3BEk5rDqNnSSDJJzIOE3+yrXIfZG0/5Dot9nvrULz2Pnu69YaipCWcUtNT1bFr6TIAKXo7BF7M1q2m3t5BlmO6bpi5AcT5GBNPgCIISyDRtT80LB8EiDlaiMsDUUFqmrTgQqDb2Z3pw==\",\"pineId\":\"STD;Bollinger_Bands\",\"pineVersion\":\"28.0\",\"in_4\":{\"v\":\"\",\"f\":true,\"t\":\"resolution\"},\"in_5\":{\"v\":true,\"f\":true,\"t\":\"bool\"},\"in_0\":{\"v\":20,\"f\":true,\"t\":\"integer\"},\"in_1\":{\"v\":\"close\",\"f\":true,\"t\":\"source\"},\"in_2\":{\"v\":2,\"f\":true,\"t\":\"float\"},\"in_3\":{\"v\":0,\"f\":true,\"t\":\"integer\"}}]}");
            waitFor(WAIT_QUICK);
            sendMessage(thread, "{\"m\":\"create_study\",\"p\":[\"" + thread.getChartSession() + "\",\"" + thread.getStudyRSI() + "\",\"st1\",\"sds_1\",\"Script@tv-scripting-101!\",{\"text\":\"p29QEj9lAJh9iuw+VOP65Q==_Rneggp8b01hBqOJXEivQHXTJxITYPEFECf2EFl4KXWSRaVE9SwE8Iovc4biw4RLHMIWzg03VnVR8FkKiFBe+L3OJX9y115b0aLBmFLPfKHdmerSijtt6B2RkhbAF9T7tSoo1Kj64RuZPoCl5IOxcfwsTMINLvIDZBvWTVouniXdzJbCQzKkfj5D1Vnc14+SSf97QaTFMzwqNcuLNVbcAY3KgoPTtMh3kJnk4kdiJk8C2gR2NxEOcX0MrT5fvXUf+hRXgcdUK5vsn8bhoB7CYOP/c8K6krOVK3cCs6NrySxkywK7leUxaNujhXz5AM82pUxrEB0DpVKZ55WwLISRb4l48N6rnqi2WPjUkH7Y9AijwAJIxan58rjTBh32IRkjA1Idc0+KYv7qUlJ2fErWMJgRwLb5i45Gbu10lyJKHcRg021FcUPVWduxaeyPUXVr+LmUjdRsnzTpSFg1VyaYyEF9ERT4gEPXTQqtVCFqEFesxmY7mVLL7Pzj1hXAlAeOtDA5MF6ts6h2Pfw2KXZo5yd0OBxtk0XS8njtg4fQJDJMNdCsQLukGcZY898h8ZCUcQZQcik3JZ+B38ImM+sTllJiltQUVMBFqPy6rmYCciwXoN9NTuPc5OrNbZwafvL3QYWGWAGtJzHZkNJ5VsIrjnmPCj1+3G8DIfI+NB0pbmPbyRLn0VzM6gSDpIlYgkLIYoPIuXPuFR55T0H5hUyMGzT6XtbnH5OvZShKzl4dWevRHTsfFxJmigyT33ORTMcIwuDLjmzICBujtInU6tqN8KkR+SZb5GUwKaM9RFuTLei2IX/kOkPFvwypBYf1QM+JMCMHSR6d+20zOmi3VNg4I3cWAwYaUkdwYXxSKwewJBloYz9CZ51erSA+saYwupXDW79LDePV5Cfgp/fai03sbvgvS8vVtmo+Ergl7g1fSCV2RRiahSq/n9iig1SCUSQNUEZGWL5AnC4/beCqzFpqVsg5a1eJMM8EPIZkI9fZDjguTu68=\",\"pineId\":\"STD;RSI\",\"pineVersion\":\"30.0\",\"in_5\":{\"v\":\"\",\"f\":true,\"t\":\"resolution\"},\"in_6\":{\"v\":true,\"f\":true,\"t\":\"bool\"},\"in_0\":{\"v\":14,\"f\":true,\"t\":\"integer\"},\"in_1\":{\"v\":\"close\",\"f\":true,\"t\":\"source\"},\"in_2\":{\"v\":\"SMA\",\"f\":true,\"t\":\"text\"},\"in_3\":{\"v\":14,\"f\":true,\"t\":\"integer\"},\"in_4\":{\"v\":2,\"f\":true,\"t\":\"float\"}}]}");
            waitFor(WAIT_QUICK);
            sendMessage(thread, "{\"m\":\"create_study\",\"p\":[\"" + thread.getChartSession() + "\",\"" + thread.getStudyTEMA() + "\",\"st1\",\"sds_1\",\"Script@tv-scripting-101!\",{\"text\":\"cwAfIpmLfrGt8bNrgB2hCw==_+Qla9wmHqX4jzJGVyTxURoQNsSjSwdKXChidtfJpyRVvFSiDzg7UW2cSBrsvApBG56uEjDtuCW6u1rJbqIUzIY1ueGe07kXIZ0Z3BVQCsQP0P9lTaubuMcVAVG/xItX3qwIP5o8V7q5NQWbEhYSfMaC73AiBSp8gxgkm7oAliBrSgLY35rG28ll8Z7b1YYu/skzDaksloOhqBV+as7dnQCLLehsU1wZ47fpS4QzV7pERl78mMDIC85zLS1bbqq5h7wokwA7zdI/Z480vIK67uMVzPN1Q9u3X4f2SZsgjOniMYSs=\",\"pineId\":\"STD;TEMA\",\"pineVersion\":\"28.0\",\"in_1\":{\"v\":\"\",\"f\":true,\"t\":\"resolution\"},\"in_2\":{\"v\":true,\"f\":true,\"t\":\"bool\"},\"in_0\":{\"v\":9,\"f\":true,\"t\":\"integer\"}}]}");
            waitFor(WAIT_QUICK);


            log.debug("Sent messages");

        }
    }

    public void addSession(FetcherThread thread) {
        while (thread.isEnabled() && !thread.getWebSocketClient().isOpen()) {
            waitFor(WAIT_QUICK);
        }
        while (ObjectUtils.isEmpty(thread.getSessionId())) {
            waitFor(WAIT_QUICK);
        }
        log.debug(thread.getThreadName() + " : Session set - " + thread.getSessionId());
    }
}
