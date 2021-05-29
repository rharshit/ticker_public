package com.ticker.fetcher.repository;

import com.ticker.fetcher.common.exception.TickerException;
import com.ticker.fetcher.common.repository.FetcherRepository;
import com.ticker.fetcher.common.repository.TickerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import static com.ticker.fetcher.common.constants.DBConstants.*;
import static com.ticker.fetcher.common.constants.ProcedureConstants.ADD_TABLE;
import static com.ticker.fetcher.common.constants.ProcedureConstants.GET_EXCHANGE_SYMBOL_ID_PR;

@Repository
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
}
