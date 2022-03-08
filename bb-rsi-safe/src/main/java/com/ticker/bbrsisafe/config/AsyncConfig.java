package com.ticker.bbrsisafe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The type Async config.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Gets fetcher async executor.
     *
     * @return the fetcher async executor
     */
    @Bean(name = "stratTaskExecutor")
    public Executor getFetcherAsyncExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Gets fetcher executor.
     *
     * @return the scheduled repo executor
     */
    @Bean(name = "fetcherExecutor")
    public Executor getFetcherExecutor() {
        return Executors.newCachedThreadPool();
    }
}