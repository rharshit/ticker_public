package com.ticker.fetcher.repository;

import com.ticker.common.repository.BaseRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

@Repository
@Getter
public class FetcherRepository extends BaseRepository {

    @Autowired
    @Qualifier("fetcherDataSource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("fetcherJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

}
