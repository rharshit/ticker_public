package com.ticker.mockfetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.common.service.BaseService;
import com.ticker.mockfetcher.model.MockFetcherRepoModel;
import com.ticker.mockfetcher.repository.MockFetcherAppRepository;
import com.ticker.mockfetcher.rx.MockFetcherThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * The type Mock fetcher service.
 */
@Service
@Slf4j
public class MockFetcherService extends BaseService {

    @Autowired
    private MockFetcherAppRepository repository;

    @Autowired
    private TickerService appService;


    @Autowired
    @Qualifier("fetcherTaskExecutor")
    private Executor fetcherTaskExecutor;

    /**
     * Set setting for the charts that are loaded
     *
     * @param fetcherThread the fetcher thread
     */
    public void setDelta(MockFetcherThread fetcherThread) {
        Timestamp curr = new Timestamp(System.currentTimeMillis());
        curr.setSeconds(0);
        long delta = fetcherThread.getStartTime() - curr.getTime();
        fetcherThread.setDelta(delta);
        fetcherThread.setInitialized(true);
    }

    /**
     * Do thread tasks.
     */
    @Scheduled(fixedDelay = 500)
    public void doThreadTasks() {
        List<MockFetcherThread> pool = appService.getCurrentTickerList();
        for (MockFetcherThread thread : pool) {
            try {
                fetcherTaskExecutor.execute(() -> doTask(thread));
            } catch (Exception e) {
                log.error(thread == null ? "" : (thread.getThreadName() + " : ") + e.getMessage());
            }

        }
    }

    /**
     * Do task.
     *
     * @param mockFetcherThread the mock fetcher thread
     */
    public void doTask(MockFetcherThread mockFetcherThread) {
        if (mockFetcherThread.isInitialized() && mockFetcherThread.isEnabled()) {
            try { // Get OHLC Value
                MockFetcherRepoModel mockFetcherRepoModel = new MockFetcherRepoModel();
                mockFetcherRepoModel.setTableName(mockFetcherThread.getTableName());
                repository.populateFetcherThreadModel(mockFetcherRepoModel, System.currentTimeMillis() + mockFetcherThread.getDelta());
                double o = mockFetcherRepoModel.getO();
                double h = mockFetcherRepoModel.getH();
                double l = mockFetcherRepoModel.getL();
                double c = mockFetcherRepoModel.getC();
                double bbA = mockFetcherRepoModel.getBbA();
                double bbU = mockFetcherRepoModel.getBbU();
                double bbL = mockFetcherRepoModel.getBbL();
                double rsi = mockFetcherRepoModel.getRsi();
                double tema = mockFetcherRepoModel.getTema();
                double dayO = mockFetcherRepoModel.getDayO();
                double dayH = mockFetcherRepoModel.getDayH();
                double dayL = mockFetcherRepoModel.getDayL();
                double dayC = mockFetcherRepoModel.getDayC();
                double prevClose = mockFetcherRepoModel.getPrevClose();
                double valCheck = o * h * l * c * bbL * bbA * bbU * rsi * dayO * dayH * dayL * dayC * prevClose;
                if (valCheck == 0) {
                    log.error(mockFetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi + "," + tema + "\n"
                            + dayO + "," + dayH + "," + dayL + "," + dayC + "," + prevClose);
                } else {
                    log.trace(mockFetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi + "," + tema + "\n"
                            + dayO + "," + dayH + "," + dayL + "," + dayC + "," + prevClose);
                    mockFetcherThread.setO(o);
                    mockFetcherThread.setH(h);
                    mockFetcherThread.setL(l);
                    mockFetcherThread.setC(c);
                    mockFetcherThread.setBbA(bbA);
                    mockFetcherThread.setBbL(bbL);
                    mockFetcherThread.setBbU(bbU);
                    mockFetcherThread.setRsi(rsi);
                    mockFetcherThread.setTema(tema);
                    mockFetcherThread.setDayO(dayO);
                    mockFetcherThread.setDayH(dayH);
                    mockFetcherThread.setDayL(dayL);
                    mockFetcherThread.setDayC(dayC);
                    mockFetcherThread.setPrevClose(prevClose);

                    mockFetcherThread.setCurrentValue(c);
                }

            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                log.debug(e.getClass().getName());
                StackTraceElement[] stackTraceElements = e.getStackTrace();
                for (StackTraceElement stackTraceElement : stackTraceElements) {
                    log.debug("\t" + stackTraceElement.toString());
                }
                log.info(mockFetcherThread.getThreadName() + " : " + e.getMessage());
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage.contains("element click intercepted")) {
                    log.info("Element click intercepted: " + mockFetcherThread.getThreadName());
                } else if (errorMessage.contains("move target out of bounds")) {
                    log.info("Move target out of bounds: " + mockFetcherThread.getThreadName());
                } else {
                    log.error("Exception in doTask(): " + mockFetcherThread.getThreadName());
                    log.error(e.getMessage());
                    throw e;
                }
            }
        } else {
            log.debug("Skipping doTask()");
        }
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
     * Gets executor details.
     *
     * @return the executor details
     */
    public Map<String, Map<String, Integer>> getExecutorDetails() {
        Map<String, Map<String, Integer>> details = new HashMap<>();
        details.put("fetcherTaskExecutor", getExecutorDetails(fetcherTaskExecutor));
        return details;
    }
}
