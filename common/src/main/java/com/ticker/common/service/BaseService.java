package com.ticker.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.ticker.common.util.Util.WAIT_QUICK;
import static com.ticker.common.util.Util.waitFor;

/**
 * The type Base service.
 */
@Service("BaseService")
@Slf4j
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
        if (taskExecutor instanceof ThreadPoolTaskExecutor) {
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
        } else if (taskExecutor instanceof ThreadPoolExecutor) {
            String valueString = getTaskExecutorValueString((ThreadPoolExecutor) taskExecutor);
            details.put("ActiveCount", Math.max(getExecutorValue(valueString, "active threads") - 1, 0));
            details.put("CorePoolSize", getExecutorValue(valueString, "pool size"));
            details.put("PoolSize", getExecutorValue(valueString, "pool size"));
            details.put("QueueSize", getExecutorValue(valueString, "queued tasks"));
        }

        return details;
    }

    private int getExecutorValue(String valueString, String key) {
        try {
            return Integer.parseInt(valueString.split("^.*\\[.*" + key + " = ")[1].split(",")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    private String getTaskExecutorValueString(ThreadPoolExecutor executor) {
        try {
            long start = System.currentTimeMillis();
            final String[] values = new String[1];
            Runnable valueFetchRunnable = () -> values[0] = executor.toString();
            executor.execute(valueFetchRunnable);
            while (values[0] == null) {
                if (System.currentTimeMillis() - start > 500) {
                    log.warn("No async executor value");
                    values[0] = executor.toString();
                } else {
                    waitFor(WAIT_QUICK);
                }
            }
            log.debug("Executor values : {}", values[0]);
            return values[0];
        } catch (Exception e) {
            return "";
        }
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
