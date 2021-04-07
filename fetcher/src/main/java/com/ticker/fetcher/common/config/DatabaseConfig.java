package com.ticker.fetcher.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.connection-timeout}")
    private long timeout;

    @Value("${spring.datasource.hikari.idle-timeout}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.minimum-idle}")
    private int minIdle;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int maxPoolSize;

    @Primary
    @Bean(name = "DataSource")
    public DataSource getDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setConnectionTimeout(timeout);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);

        return new HikariDataSource(hikariConfig);
    }

    @Bean(name = "jdbcTemplate")
    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(getDataSource());
    }
}
