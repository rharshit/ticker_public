package com.ticker.mockfetcher.service;

import com.ticker.common.exception.TickerException;
import com.ticker.mockfetcher.common.rx.MockFetcherThread;
import com.ticker.mockfetcher.model.MockFetcherRepoModel;
import com.ticker.mockfetcher.repository.MockFetcherAppRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class MockFetcherService {

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
     * @param fetcherThread
     */
    public void setDelta(MockFetcherThread fetcherThread) {
        Timestamp curr = new Timestamp(System.currentTimeMillis());
        curr.setSeconds(0);
        long delta = fetcherThread.getStartTime() - curr.getTime();
        fetcherThread.setDelta(delta);
        fetcherThread.setInitialized(true);
    }

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

    public void doTask(MockFetcherThread mockFetcherThread) {
        if (mockFetcherThread.isInitialized() && mockFetcherThread.isEnabled()) {
            try { // Get OHLC Value
                MockFetcherRepoModel mockFetcherRepoModel = new MockFetcherRepoModel();
                mockFetcherRepoModel.setTableName(mockFetcherThread.getTableName());
                repository.populateFetcherThreadModel(mockFetcherRepoModel, System.currentTimeMillis() + mockFetcherThread.getDelta());
                float o = mockFetcherRepoModel.getO();
                float h = mockFetcherRepoModel.getH();
                float l = mockFetcherRepoModel.getL();
                float c = mockFetcherRepoModel.getC();
                float bbA = mockFetcherRepoModel.getBbA();
                float bbU = mockFetcherRepoModel.getBbU();
                float bbL = mockFetcherRepoModel.getBbL();
                float rsi = mockFetcherRepoModel.getRsi();
                float tema = mockFetcherRepoModel.getTema();
                float valCheck = o * h * l * c * bbL * bbA * bbU * rsi;
                if (valCheck == 0) {
                    log.error(mockFetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi + "," + tema);
                } else {
                    log.trace(mockFetcherThread.getThreadName() + " :\n" +
                            o + "," + h + "," + l + "," + c + "\n"
                            + bbL + "," + bbA + "," + bbU + "\n"
                            + rsi + "," + tema);
                    mockFetcherThread.setO(o);
                    mockFetcherThread.setH(h);
                    mockFetcherThread.setL(l);
                    mockFetcherThread.setC(c);
                    mockFetcherThread.setBbA(bbA);
                    mockFetcherThread.setBbL(bbL);
                    mockFetcherThread.setBbU(bbU);
                    mockFetcherThread.setRsi(rsi);

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
