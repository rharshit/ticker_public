package com.ticker.common.service;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * The type Base service.
 */
@Service("BaseService")
public abstract class BaseService {

    public Map<String, Map<String, Integer>> getExecutorDetailMap() {
        Map<String, Map<String, Integer>> details = new HashMap<>();
        for (Map.Entry<String, Executor> executor : getExecutorMap().entrySet()) {
            details.put(executor.getKey(), getExecutorDetails(executor.getValue()));
        }
        return details;
    }

    protected abstract Map<String, Executor> getExecutorMap();

    /**
     * Gets executor details.
     *
     * @param taskExecutor the task executor
     * @return the executor details
     */
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
        details.put("QueueSize", ((ThreadPoolTaskExecutor) taskExecutor).getThreadPoolExecutor().getQueue().size());
        return details;
    }

    /**
     * Gets memory statistics.
     *
     * @return the memory statistics
     */
    public Map<String, String> getMemoryStatistics() {
        Map<String, String> stats = new HashMap<>();
        stats.put("HeapSize", String.valueOf(Runtime.getRuntime().totalMemory()));
        stats.put("HeapMaxSize", String.valueOf(Runtime.getRuntime().maxMemory()));
        stats.put("HeapFreeSize", String.valueOf(Runtime.getRuntime().freeMemory()));
        return stats;
    }
}
