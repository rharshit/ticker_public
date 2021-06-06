package com.ticker.fetcher.repository;

import com.ticker.fetcher.common.exception.TickerException;
import com.ticker.fetcher.common.repository.FetcherRepository;
import com.ticker.fetcher.common.repository.TickerRepository;
import com.ticker.fetcher.model.FetcherRepoModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ticker.fetcher.common.constants.DBConstants.*;
import static com.ticker.fetcher.common.constants.ProcedureConstants.ADD_TABLE;
import static com.ticker.fetcher.common.constants.ProcedureConstants.GET_EXCHANGE_SYMBOL_ID_PR;

@Repository
@Slf4j
public class AppRepository {

    @Autowired
    FetcherRepository fetcherRepository;

    @Autowired
    TickerRepository tickerRepository;


    public int getExchangeSymbolId(String exchange, String symbol) {
        SqlParameter[] sqlParameters = {
                new SqlParameter(I_EXCHANGE_ID, Types.VARCHAR),
                new SqlParameter(I_SYMBOL_ID, Types.VARCHAR),
                new SqlOutParameter(O_EXCHANGE_SYMBOL_ID, Types.INTEGER)
        };

        Map<String, Object> inputParameters = new HashMap<>();
        inputParameters.put(I_EXCHANGE_ID, exchange);
        inputParameters.put(I_SYMBOL_ID, symbol);

        Map<String, Object> result = tickerRepository.executeProcedure(GET_EXCHANGE_SYMBOL_ID_PR, sqlParameters, inputParameters);
        return (int) result.get(O_EXCHANGE_SYMBOL_ID);
    }

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

    @Async("repoExecutor")
    public void pushData(List<FetcherRepoModel> dataQueue, String sNow) {
        log.info("pushData task started: " + sNow);
        log.info("Pushing data, size: " + dataQueue.size());
        try {
            if (!CollectionUtils.isEmpty(dataQueue)) {
                Connection connection = fetcherRepository.getDataSource().getConnection();
                Statement statement = connection.createStatement();
                for (FetcherRepoModel data : dataQueue) {
                    log.debug(data.toString());
                    String deleteSql = "DELETE FROM " + data.getTableName() + " WHERE `timestamp`='" + data.getTimestamp() + "'";
                    log.debug(deleteSql);
                    statement.addBatch(deleteSql);
                    String insertSql = "INSERT INTO " + data.getTableName() + " (`timestamp`, O, H, L, C, BB_U, BB_A, BB_L, EXTRA)" +
                            "VALUES('" + data.getTimestamp() + "', " + data.getO() + ", " + data.getH() +
                            ", " + data.getL() + ", " + data.getC() + ", " + data.getBbU() + ", " +
                            data.getBbA() + ", " + data.getBbL() + ", " + data.getExtra() + ")";
                    log.debug(insertSql);
                    statement.addBatch(insertSql);
                }
                int[] count = statement.executeBatch();
                log.debug("Data pushed");
            }
        } catch (Exception e) {
            log.error("Error while pushing data to DB");
            log.error(e.getMessage());
            throw new TickerException("Error while pushing data to DB");
        }

        log.debug("Pushed data");
        log.debug("pushData task ended: " + sNow);
    }
}
