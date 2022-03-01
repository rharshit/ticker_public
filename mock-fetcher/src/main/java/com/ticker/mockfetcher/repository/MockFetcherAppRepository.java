package com.ticker.mockfetcher.repository;

import com.ticker.common.exception.TickerException;
import com.ticker.mockfetcher.model.MockFetcherRepoModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ticker.mockfetcher.constants.DBConstants.TABLE_NAME;
import static com.ticker.mockfetcher.constants.ProcedureConstants.ADD_TABLE;

/**
 * The type Mock fetcher app repository.
 */
@Repository
@Slf4j
public class MockFetcherAppRepository {

    private final Connection fetcherConnection = null;
    /**
     * The Fetcher repository.
     */
    @Autowired
    FetcherRepository fetcherRepository;

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

    /**
     * Populate fetcher thread model.
     *
     * @param fetcherRepoModel the fetcher repo model
     * @param timestamp        the timestamp
     */
    public void populateFetcherThreadModel(MockFetcherRepoModel fetcherRepoModel, long timestamp) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(timestamp));
        String query = "SELECT * FROM " + fetcherRepoModel.getTableName() + " WHERE `timestamp`='" + time + "'";
        try {
            log.trace(query);
            List<Map<String, Object>> result = fetcherRepository.queryForList(query);
            fetcherRepoModel.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format((Timestamp) result.get(0).get("timestamp")));
            fetcherRepoModel.setO((Float) result.get(0).get("O"));
            fetcherRepoModel.setH((Float) result.get(0).get("H"));
            fetcherRepoModel.setL((Float) result.get(0).get("L"));
            fetcherRepoModel.setC((Float) result.get(0).get("C"));
            fetcherRepoModel.setBbU((Float) result.get(0).get("BB_U"));
            fetcherRepoModel.setBbA((Float) result.get(0).get("BB_A"));
            fetcherRepoModel.setBbL((Float) result.get(0).get("BB_L"));
            fetcherRepoModel.setRsi((Float) result.get(0).get("RSI"));
            fetcherRepoModel.setTema((Float) result.get(0).get("TEMA"));
            fetcherRepoModel.setDayO((Float) result.get(0).get("DAY_O"));
            fetcherRepoModel.setDayH((Float) result.get(0).get("DAY_H"));
            fetcherRepoModel.setDayL((Float) result.get(0).get("DAY_L"));
            fetcherRepoModel.setDayC((Float) result.get(0).get("DAY_C"));
            fetcherRepoModel.setPrevClose((Float) result.get(0).get("PREV_CLOSE"));
        } catch (IndexOutOfBoundsException e) {
            log.error("Cannot fetch from " + fetcherRepoModel.getTableName() + " for " + time);
        } catch (Exception e) {
            log.error(query);
            log.debug(e.getMessage(), e);
        }
    }
}
