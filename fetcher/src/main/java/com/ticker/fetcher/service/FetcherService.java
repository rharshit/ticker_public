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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

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

    /**
     * On message received.
     *
     * @param thread the thread
     * @param data   the data
     */
    public void onReceiveMessage(FetcherThread thread, String data) {
        log.info("Recv:\n" + data);
        fetcherTaskExecutor.execute(() -> {
            String[] messages = data.split("~m~\\d*~m~");
            for (String message : messages) {
                try {
                    JSONObject object = new JSONObject(message);
                    if (object.has("p")) {
                        JSONArray array = object.getJSONArray("p");
                        for (int i = 0; i < array.length(); i++) {
                            try {
                                String objString = array.get(i).toString();
                                JSONObject jsonObject = new JSONObject(objString);
                                if (jsonObject.has(thread.getStudySeries()) || jsonObject.has(thread.getStudyBB()) || jsonObject.has(thread.getStudyRSI()) || jsonObject.has(thread.getStudyTEMA())) {
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
                thread.setUpdatedAt(System.currentTimeMillis());
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
}
