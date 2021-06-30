package com.ticker.mockfetcher.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@Configuration
public class FetcherDatabaseConfig {

    @Value("${spring.datasource.fetcher.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.fetcher.username}")
    private String username;

    @Value("${spring.datasource.fetcher.password}")
    private String password;

    @Value("${spring.datasource.hikari.fetcher.connection-timeout}")
    private long timeout;

    @Value("${spring.datasource.hikari.fetcher.idle-timeout}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.fetcher.max-lifetime}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.fetcher.minimum-idle}")
    private int minIdle;

    @Value("${spring.datasource.hikari.fetcher.maximum-pool-size}")
    private int maxPoolSize;

    @Bean(name = "fetcherDataSource")
    public DataSource getTickerDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setConnectionTimeout(timeout);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setPoolName("HikariPoolFetcher");

        return new HikariDataSource(hikariConfig);
    }

    @Bean(name = "fetcherJdbcTemplate")
    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(getTickerDataSource());
    }
}
