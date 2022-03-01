package com.ticker.fetcher.repository;

import com.ticker.common.exception.TickerException;
import com.ticker.fetcher.model.FetcherRepoModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.ticker.fetcher.constants.DBConstants.TABLE_NAME;
import static com.ticker.fetcher.constants.FetcherConstants.FETCHER_DATE_FORMAT_LOGGING;
import static com.ticker.fetcher.constants.ProcedureConstants.ADD_TABLE;

/**
 * The type Fetcher app repository.
 */
@Repository
@Slf4j
public class FetcherAppRepository {

    private static final List<String> sqlQueue = new ArrayList<>();
    @Autowired
    private FetcherRepository fetcherRepository;
    @Autowired
    @Qualifier("repoExecutor")
    private Executor repoExecutor;
    private Connection fetcherConnection = null;
    private boolean pushing = false;

    /**
     * Add table.
     *
     * @param tableName the table name
     */
    public void addTable(String tableName) {
        try {
            SqlParameter[] parameters = {
                    new SqlParameter(TABLE_NAME, Types.VARCHAR)
            };
            Map<String, Object> inputParameters = new HashMap<>();
            inputParameters.put(TABLE_NAME, tableName);
            fetcherRepository.executeProcedure(ADD_TABLE, parameters, inputParameters);
        } catch (Exception e) {
            throw new TickerException("Error while creating table " + tableName);
        }


    }

    private Connection getFetcherConnection() {
        try {
            if (this.fetcherConnection == null || this.fetcherConnection.isClosed()) {
                this.fetcherConnection = this.fetcherRepository.getDataSource().getConnection();
            }
            return this.fetcherConnection;
        } catch (SQLException throwables) {
            return null;
        }
    }

    /**
     * Push data.
     */
    @Scheduled(fixedRate = 5000)
    public void pushData() {
        if (!pushing) {
            pushing = true;
            repoExecutor.execute(() -> {
                try {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(FETCHER_DATE_FORMAT_LOGGING);
                    String now = dtf.format(LocalDateTime.now());
                    log.trace("pushData task started: " + now);
                    List<String> tempQueue;
                    synchronized (sqlQueue) {
                        tempQueue = new ArrayList<>(sqlQueue);
                        sqlQueue.clear();
                    }
                    long start = System.currentTimeMillis();
                    if (!CollectionUtils.isEmpty(tempQueue)) {
                        try (Connection connection = getFetcherConnection()) {
                            Statement statement = connection.createStatement();
                            log.debug("Pushing data, size: " + tempQueue.size());
                            for (String sql : tempQueue) {
                                log.trace(sql);
                                statement.addBatch(sql);
                            }
                            statement.executeBatch();
                            log.trace("Executed queries");
                            log.trace("Data pushed");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            log.error("Data not pushed");
                            log.info("Adding sql data back to queue");
                            synchronized (sqlQueue) {
                                sqlQueue.addAll(tempQueue);
                            }
                        }
                    }
                    long end = System.currentTimeMillis();
                    log.debug("Pushing data to repo took " + (end - start) + "ms");
                    log.debug("pushData task ended: " + now);
                } catch (Exception ignore) {

                }
                pushing = false;
            });
        }
    }

    /**
     * Add to queue.
     *
     * @param datas the datas
     * @param sNow  the s now
     */
    @Async
    public void addToQueue(List<FetcherRepoModel> datas, String sNow) {
        log.trace("addToQueue task started: " + sNow);
        log.trace("Adding data, size: " + datas.size());
        if (!CollectionUtils.isEmpty(datas)) {
            List<String> tempQueue = new ArrayList<>();
            for (FetcherRepoModel data : datas) {
                log.trace(data.toString());
                String deleteSql = "DELETE FROM " + data.getTableName() + " WHERE `timestamp`='" + data.getTimestamp() + "'";
                log.trace(deleteSql);
                tempQueue.add(deleteSql);
                String insertSql = "INSERT INTO " + data.getTableName() +
                        " (`timestamp`, O, H, L, C, BB_U, BB_A, BB_L, RSI, TEMA, DAY_O, DAY_H, DAY_L, DAY_C, PREV_CLOSE)" +
                        "VALUES('" + data.getTimestamp() + "', " + data.getO() + ", " + data.getH() +
                        ", " + data.getL() + ", " + data.getC() + ", " + data.getBbU() + ", " +
                        data.getBbA() + ", " + data.getBbL() + ", " + data.getRsi() + ", " + data.getTema() +
                        ", " + data.getDayO() + ", " + data.getDayH() + ", " + data.getDayL() + ", " + data.getDayC() +
                        ", " + data.getPrevClose() + ")";
                log.trace(insertSql);
                tempQueue.add(insertSql);
            }
            synchronized (sqlQueue) {
                log.debug("Initial data, size: " + sqlQueue.size());
                sqlQueue.addAll(tempQueue);
                log.debug("Data Added, size: " + sqlQueue.size());
            }
        }
        log.trace("addToQueue task ended: " + sNow);
    }
}
