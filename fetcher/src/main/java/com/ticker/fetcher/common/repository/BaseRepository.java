package com.ticker.fetcher.common.repository;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.GenericStoredProcedure;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Map;

@Repository
@Slf4j
@Setter
public abstract class BaseRepository {

    protected void setDatasource(StoredProcedure procedure) {
        procedure.setDataSource(getDataSource());
    }

    protected String getDatasourceName() {
        return ((HikariDataSource) getDataSource()).getPoolName();
    }

    public Map<String, Object> executeProcedure(String procedureName, SqlParameter[] sqlParameters, Map<String, Object> inputParameters) {
        StoredProcedure procedure = new GenericStoredProcedure();
        setDatasource(procedure);
        procedure.setSql(procedureName);
        procedure.setFunction(false);
        procedure.setParameters(sqlParameters);

        log.debug("Running procedure : " + getDatasourceName());

        return procedure.execute(inputParameters);
    }

    protected abstract DataSource getDataSource();
}
