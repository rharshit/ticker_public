package com.ticker.mockfetcher.repository;

import com.ticker.common.exception.TickerException;
import com.ticker.mockfetcher.common.repository.FetcherRepository;
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

import static com.ticker.mockfetcher.common.constants.DBConstants.TABLE_NAME;
import static com.ticker.mockfetcher.common.constants.ProcedureConstants.ADD_TABLE;

@Repository
@Slf4j
public class MockFetcherAppRepository {

    @Autowired
    FetcherRepository fetcherRepository;

    private final Connection fetcherConnection = null;

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
        } catch (IndexOutOfBoundsException e) {
            log.error("Cannot fetch from " + fetcherRepoModel.getTableName() + " for " + time);
        } catch (Exception e) {
            log.error(query);
            log.debug(e.getMessage(), e);
        }
    }
}
