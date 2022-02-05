package com.ticker.common.service;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
public class BaseService {
    protected Map<String, Integer> getExecutorDetails(Executor taskExecutor) {
        Map<String, Integer> details = new HashMap<>();
        details.put("ActiveCount", ((ThreadPoolTaskExecutor) taskExecutor).getActiveCount());
        details.put("CorePoolSize", ((ThreadPoolTaskExecutor) taskExecutor).getCorePoolSize());
        details.put("MaxPoolSize", ((ThreadPoolTaskExecutor) taskExecutor).getMaxPoolSize());
        details.put("PoolSize", ((ThreadPoolTaskExecutor) taskExecutor).getPoolSize());
        details.put("ExecutorActiveCount", ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getActiveCount());
        details.put("ExecutorCorePoolSize", ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getCorePoolSize());
        details.put("MaximumPoolSize", ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getMaximumPoolSize());
        details.put("ExecutorPoolSize", ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getPoolSize());
        details.put("LargestPoolSize", ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getLargestPoolSize());
        details.put("TaskCount", (int) ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getTaskCount());
        return details;
    }
}
