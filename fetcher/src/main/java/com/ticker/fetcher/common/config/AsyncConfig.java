package com.ticker.fetcher.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
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

    @Bean(name = "repoExecutor")
    public Executor getRepoAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("repoExec-");
        executor.initialize();
        return executor;
    }

}