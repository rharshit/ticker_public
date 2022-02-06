package com.ticker.mockfetcher.config;

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
    @Bean(name = "fetcherTaskExecutor")
    public Executor getFetcherAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(32);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("fetcherExec-");
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
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("schedExec-");
        executor.initialize();
        return executor;
    }

    /**
     * Gets repo async executor.
     *
     * @return the repo async executor
     */
    @Bean(name = "repoExecutor")
    public Executor getRepoAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("repoExec-");
        executor.setRejectedExecutionHandler((r, executor1) -> log.info("Rejected thread: " + r.toString()));
        executor.initialize();
        return executor;
    }

}