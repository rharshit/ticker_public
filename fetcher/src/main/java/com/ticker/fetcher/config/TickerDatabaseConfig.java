package com.ticker.fetcher.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


/**
 * The type Ticker database config.
 */
@Configuration
public class TickerDatabaseConfig {

    @Value("${spring.datasource.ticker.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.ticker.username}")
    private String username;

    @Value("${spring.datasource.ticker.password}")
    private String password;

    @Value("${spring.datasource.hikari.ticker.connection-timeout}")
    private long timeout;

    @Value("${spring.datasource.hikari.ticker.idle-timeout}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.ticker.max-lifetime}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.ticker.minimum-idle}")
    private int minIdle;

    @Value("${spring.datasource.hikari.ticker.maximum-pool-size}")
    private int maxPoolSize;

    /**
     * Gets ticker data source.
     *
     * @return the ticker data source
     */
    @Bean(name = "tickerDataSource")
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
        hikariConfig.setPoolName("HikariPoolTicker");

        return new HikariDataSource(hikariConfig);
    }

    /**
     * Gets jdbc template.
     *
     * @return the jdbc template
     */
    @Bean(name = "tickerJdbcTemplate")
    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(getTickerDataSource());
    }
}
