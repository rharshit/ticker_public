package com.ticker.fetcher.common.repository;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.GenericStoredProcedure;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Map;

@Repository
@Getter
public class FetcherRepository {

    @Autowired
    @Qualifier("tickerDataSource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("tickerJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private void setDatasource(StoredProcedure procedure) {
        procedure.setDataSource(dataSource);
    }

    public Map<String, Object> executeProcedure(String procedureName, SqlParameter[] sqlParameters, Map<String, Object> inputParameters) {
        StoredProcedure procedure = new GenericStoredProcedure();
        setDatasource(procedure);
        procedure.setSql(procedureName);
        procedure.setFunction(false);
        procedure.setParameters(sqlParameters);
        return procedure.execute(inputParameters);
    }
}
