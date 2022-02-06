package com.ticker.fetcher.repository;

import com.ticker.common.exception.TickerException;
import com.ticker.fetcher.model.FetcherRepoModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.SqlParameter;
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

@Repository
@Slf4j
public class FetcherAppRepository {

    @Autowired
    private FetcherRepository fetcherRepository;

    @Autowired
    @Qualifier("repoExecutor")
    private Executor repoExecutor;

    private static final List<String> sqlQueue = new ArrayList<>();
    private Connection fetcherConnection = null;

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

    @Scheduled(fixedRate = 1000)
    public void pushData() {
        repoExecutor.execute(() -> {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(FETCHER_DATE_FORMAT_LOGGING);
            String now = dtf.format(LocalDateTime.now());
            log.debug("pushData task started: " + now);
            if (!CollectionUtils.isEmpty(sqlQueue)) {
                try (Connection connection = getFetcherConnection()) {
                    Statement statement = connection.createStatement();
                    synchronized (sqlQueue) {
                        log.debug("Pushing data, size: " + sqlQueue.size());
                        for (String sql : sqlQueue) {
                            log.trace(sql);
                            statement.addBatch(sql);
                        }
                        sqlQueue.clear();
                    }
                    statement.executeBatch();
                    log.debug("Executed queries");
                    log.debug("Data pushed");
                } catch (SQLException e) {
                    e.printStackTrace();
                    log.error("Data not pushed");
                }
            }
            log.debug("pushData task ended: " + now);
        });
    }

    public void addToQueue(List<FetcherRepoModel> datas, String sNow) {
        log.debug("addToQueue task started: " + sNow);
        log.debug("Adding data, size: " + datas.size());
        if (!CollectionUtils.isEmpty(datas)) {
            log.debug("Initial data, size: " + sqlQueue.size());
            for (FetcherRepoModel data : datas) {
                synchronized (sqlQueue) {
                    log.trace(data.toString());
                    String deleteSql = "DELETE FROM " + data.getTableName() + " WHERE `timestamp`='" + data.getTimestamp() + "'";
                    log.trace(deleteSql);
                    sqlQueue.add(deleteSql);
                    String insertSql = "INSERT INTO " + data.getTableName() + " (`timestamp`, O, H, L, C, BB_U, BB_A, BB_L, RSI, TEMA)" +
                            "VALUES('" + data.getTimestamp() + "', " + data.getO() + ", " + data.getH() +
                            ", " + data.getL() + ", " + data.getC() + ", " + data.getBbU() + ", " +
                            data.getBbA() + ", " + data.getBbL() + ", " + data.getRsi() + ", " + data.getTema() + ")";
                    log.trace(insertSql);
                    sqlQueue.add(insertSql);
                }
            }
            log.debug("Data Added, size: " + sqlQueue.size());
        }
        log.debug("addToQueue task ended: " + sNow);
    }
}
