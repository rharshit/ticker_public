package com.ticker.mockfetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.mockfetcher.common.rx.MockFetcherThread;
import com.ticker.mockfetcher.model.FetcherRepoModel;
import com.ticker.mockfetcher.repository.FetcherAppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FetcherService {

    private static final List<FetcherRepoModel> dataQueue = new ArrayList<>();
    @Autowired
    FetcherAppRepository repository;
    @Autowired
    private TickerService appService;

    /**
     * Set setting for the charts that are loaded
     *
     * @param fetcherThread
     */
    public void setDelta(MockFetcherThread fetcherThread) {
        Timestamp curr = new Timestamp(System.currentTimeMillis());
        curr.setSeconds(0);
        long delta = fetcherThread.getStartTime() - curr.getTime();
        fetcherThread.setDelta(delta);
        fetcherThread.setInitialized(true);
    }

    @Async("fetcherTaskExecutor")
    @Scheduled(fixedRate = 750)
    public void doThreadTasks() {
        List<MockFetcherThread> pool = appService.getCurrentTickerList();
        for (MockFetcherThread thread : pool) {
            try {
                doTask(thread);
            } catch (Exception e) {
                log.error(thread == null ? "" : (thread.getThreadName() + " : ") + e.getMessage());
            }

        }
    }

    @Async("fetcherTaskExecutor")
    public void doTask(MockFetcherThread fetcherThread) {
        if (fetcherThread.isInitialized() && fetcherThread.isEnabled()) {
            try { // Get current value

            } catch (Exception ignored) {
            }
            try { // Get OHLC Value
                float o = 0;
                float h = 0;
                float l = 0;
                float c = 0;
                float bbA = 0;
                float bbU = 0;
                float bbL = 0;
                float rsi = 0;
                float valCheck = o * h * l * c * bbL * bbA * bbU * rsi;
                if (valCheck == 0) {
                    log.error(fetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi);
                } else {
                    log.trace(fetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi);
                    synchronized (dataQueue) {
                        dataQueue.add(new FetcherRepoModel(fetcherThread.getTableName(), System.currentTimeMillis(),
                                o, h, l, c, bbU, bbA, bbL, rsi));
                    }
                    log.debug("doTask() added data: " + fetcherThread.getThreadName() + ", size: " + dataQueue.size());
                }

            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                log.debug(e.getClass().getName());
                StackTraceElement[] stackTraceElements = e.getStackTrace();
                for (StackTraceElement stackTraceElement : stackTraceElements) {
                    log.debug("\t" + stackTraceElement.toString());
                }
                log.info(fetcherThread.getThreadName() + " : " + e.getMessage());
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage.contains("element click intercepted")) {
                    log.info("Element click intercepted: " + fetcherThread.getThreadName());
                } else if (errorMessage.contains("move target out of bounds")) {
                    log.info("Move target out of bounds: " + fetcherThread.getThreadName());
                } else {
                    log.error("Exception in doTask(): " + fetcherThread.getThreadName());
                    log.error(e.getMessage());
                    throw e;
                }
            }
        } else {
            log.debug("Skipping doTask()");
        }
    }

    @Async("scheduledExecutor")
    @Scheduled(fixedRate = 850)
    public void scheduledJob() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String sNow = dtf.format(now);
        log.debug("Scheduled task started: " + sNow);
        List<FetcherRepoModel> tempDataQueue;
        synchronized (dataQueue) {
            tempDataQueue = new ArrayList<>(dataQueue);
            dataQueue.clear();
        }
        log.debug("Scheduled task populated: " + sNow);
        repository.addToQueue(tempDataQueue, sNow);
        log.debug("Scheduled task ended: " + sNow);
    }

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
