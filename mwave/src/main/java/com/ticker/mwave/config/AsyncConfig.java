package com.ticker.mwave.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * The type Async config.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(5);
        executor.setThreadNamePrefix("appExecutor-");
        executor.initialize();
        return executor;
    }

    /**
     * Gets fetcher async executor.
     *
     * @return the fetcher async executor
     */
    @Bean(name = "stratTaskExecutor")
    public Executor getFetcherAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(32);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("stratExec-");
        executor.initialize();
        return executor;
    }

    /**
     * Gets scheduled repo executor.
     *
     * @return the scheduled repo executor
     */
    @Bean(name = "scheduledExecutor")
    public Executor getScheduledRepoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("schedExec-");
        executor.initialize();
        return executor;
    }
}