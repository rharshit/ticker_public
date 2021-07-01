package com.ticker.common.repository;

import com.ticker.common.exception.TickerException;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.GenericStoredProcedure;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
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

    protected abstract DataSource getDataSource();

    protected abstract JdbcTemplate getJdbcTemplate();

    public Map<String, Object> executeProcedure(String procedureName, SqlParameter[] sqlParameters, Map<String, Object> inputParameters) {
        StoredProcedure procedure = new GenericStoredProcedure();
        setDatasource(procedure);
        procedure.setSql(procedureName);
        procedure.setFunction(false);
        procedure.setParameters(sqlParameters);

        log.debug("Running procedure : " + getDatasourceName());

        return procedure.execute(inputParameters);
    }


    /**
     * Executes sql query.
     *
     * @param sqlQuery
     * @param rowMapper
     * @return
     */
    public List executeQuery(final String sqlQuery, final RowMapper rowMapper) {
        try {
            log.debug("sqlQuery : " + sqlQuery);
            return getJdbcTemplate().query(sqlQuery, rowMapper);
        } catch (DataAccessException e) {
            log.error("Error while executing query");
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes sql query.
     *
     * @param sqlQuery
     * @param rowMapper
     * @param args
     * @return
     * @throws java.lang.Exception
     */
    public List executeQuery(final String sqlQuery, final RowMapper rowMapper, final Object... args) {
        log.debug("sqlQuery : " + sqlQuery);
        log.debug("args : " + args);

        if (StringUtils.countOccurrencesOf(sqlQuery, "?") != args.length) {
            throw new TickerException("Invalid Arguments");
        }
        try {
            return getJdbcTemplate().query(sqlQuery, args, rowMapper);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes sql query.
     *
     * @param sqlQuery
     */
    public void executeQuery(final String sqlQuery) {
        try {
            log.debug("sqlQuery : " + sqlQuery);
            getJdbcTemplate().execute(sqlQuery);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes sql query.
     *
     * @param sqlQuery
     * @param args
     */
    public void executeQuery(final String sqlQuery, final Object... args) {
        try {
            log.debug("sqlQuery : " + sqlQuery);
            log.debug("args : " + args);
            getJdbcTemplate().update(sqlQuery, args);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes sql query.
     *
     * @param <T>
     * @param sqlQuery
     * @param requiredType
     * @param args
     * @return
     */
    public <T> T queryForObject(final String sqlQuery, Class<T> requiredType, final Object... args) {
        try {
            return getJdbcTemplate().queryForObject(sqlQuery, requiredType, args);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes the query and return the object provided in the rowmapper.
     *
     * @param <T>
     * @param sqlQuery
     * @param rowMapper
     * @param args
     * @return
     */
    public <T> T queryForObject(final String sqlQuery, RowMapper<T> rowMapper, final Object... args) {
        return getJdbcTemplate().queryForObject(sqlQuery, rowMapper, args);
    }

    /**
     * Executes the query and return the map of return value
     *
     * @param sqlQuery
     * @param args
     * @return
     */
    public List<Map<String, Object>> queryForList(final String sqlQuery, final Object... args) {
        try {
            return getJdbcTemplate().queryForList(sqlQuery, args);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes the query and return the list of object provided in the class.
     *
     * @param sqlQuery
     * @param requiredType
     * @param args
     * @param <T>
     * @return
     */
    public <T> List<T> queryForList(final String sqlQuery, Class<T> requiredType, final Object... args) {
        try {
            return getJdbcTemplate().queryForList(sqlQuery, requiredType, args);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes the query and return the map of return value
     *
     * @param sqlQuery
     * @param args
     * @return
     * @author RanganathPrasad
     */
    public <T> List<T> queryForList(final String sqlQuery, RowMapper<T> rowMapper, final Object... args) {
        try {
            return getJdbcTemplate().query(sqlQuery, rowMapper, args);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

    /**
     * Executes sql query.
     *
     * @param sqlQuery
     */
    public void executeQueryForUpdate(final String sqlQuery) {
        try {
            getJdbcTemplate().update(sqlQuery);
        } catch (DataAccessException e) {
            log.error(String.valueOf(e));
            log.error(sqlQuery);
            throw new TickerException(e.getMessage());
        }
    }

}
