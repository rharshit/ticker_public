package com.ticker.common.util.objectpool;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ObjectPool<D extends ObjectPoolData<?>> {
    private final int min;
    private final int idle;
    private final int max;
    private final long validationTime;
    private final long idleTime;
    private final Set<ObjectPoolData<?>> pool;

    private boolean shutdown;

    public ObjectPool(int min, int idle, int max, long validationTime, long idleTimeout) {
        log.info("Object pool  - Starting...");
        this.min = min;
        this.idle = idle;
        this.max = max;
        this.validationTime = validationTime;
        this.idleTime = idleTimeout;

        pool = Collections.newSetFromMap(new ConcurrentHashMap<>());

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this::validate, 0, validationTime, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroyPool));
        log.info("Object pool  - Start completed.");
    }

    /**
     * Get pool size
     *
     * @return An array with values [idle, valid, total]
     */
    public int[] poolSize() {
        int idle = 0;
        int valid = 0;
        int invalid = 0;
        for (ObjectPoolData<?> object : pool) {
            if (object.isIdle()) {
                idle++;
            } else if (object.isValid()) {
                valid++;
            } else {
                invalid++;
            }
        }
        return new int[]{idle, valid, pool.size()};
//        log.info("Total:\t" + pool.size() + "\tIdle:\t" + idle + "\tValid:\t" + valid + "\tInvalid:\t" + invalid);
    }

    public abstract D createObject();

    private void destroyPool() {
        log.info("Object pool - Shutdown initiated...");
        shutdown = true;
        List<Thread> threads = new ArrayList<>();
        for (ObjectPoolData<?> object : pool) {
            Thread thread = new Thread(object::destroy);
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Object pool - Shutdown completed.");
    }

    private void removeObject(ObjectPoolData<?> object) {
        pool.remove(object);
        object.destroy();
    }

    private void validate() {
        if (shutdown) {
            return;
        }
        for (ObjectPoolData<?> object : pool) {
            if (object.isValid() && !object.isIdle()) {
                object.setLastUsed(System.currentTimeMillis());
            }
        }
        if (pool.size() > idle) {
            int toDestroy = pool.size() - idle;
            synchronized (pool) {
                for (ObjectPoolData<?> object : pool) {
                    if (toDestroy == 0) {
                        break;
                    }
                    if (object.isInitializingObject()) {
                        continue;
                    }
                    if (object.isValid()) {
                        if (object.isIdle()) {
                            if (toDestroy > 0) {
                                removeObject(object);
                                toDestroy--;
                            }
                        } else {
                            object.setLastUsed(System.currentTimeMillis());
                        }
                    } else {
                        removeObject(object);
                        toDestroy--;
                    }
                }
            }

        }
        if (pool.size() > min) {
            int toReduce = pool.size() - min;
            synchronized (pool) {
                for (ObjectPoolData<?> object : pool) {
                    if (toReduce == 0) {
                        break;
                    }
                    if (object.isInitializingObject()) {
                        continue;
                    }
                    if (object.isValid()) {
                        if (object.isIdle() && System.currentTimeMillis() - object.getLastUsed() > idleTime) {
                            if (toReduce > 0) {
                                removeObject(object);
                                toReduce--;
                            }
                        }
                    } else {
                        removeObject(object);
                        toReduce--;
                    }
                }
            }
        }
        if (pool.size() < min) {
            int toAdd = min - pool.size();
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < toAdd; i++) {
                Thread thread = new Thread(() -> {
                    pool.add(createObject());
                });
                thread.start();
                threads.add(thread);
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Object get() {
        do {
            synchronized (pool) {
                for (ObjectPoolData<?> object : pool) {
                    if (object.isValid() && object.isIdle()) {
                        object.setIdle(false);
                        return object.getObject();
                    }
                }
                if (pool.size() < max) {
                    pool.add(createObject());
                }
                try {
                    this.wait(validationTime);
                } catch (InterruptedException ignored) {
                }
            }
        } while (true);
    }

    public void put(Object data) {
        synchronized (pool) {
            for (ObjectPoolData<?> object : pool) {
                if (object.equalsObject(data)) {
                    object.setIdle(true);
                    break;
                }
            }
        }
    }

}
