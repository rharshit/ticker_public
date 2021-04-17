package com.ticker.fetcher.common.repository;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
@Getter
public class TickerRepository extends BaseRepository {

    @Autowired
    @Qualifier("tickerDataSource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("tickerJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

}
