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

/**
 * The type Object pool.
 *
 * @param <D> the type parameter
 */
@Slf4j
public abstract class ObjectPool<D extends ObjectPoolData<?>> {
    private final int min;
    private final int idle;
    private final int max;
    private final long validationTime;
    private final long idleTime;
    private final Set<ObjectPoolData<?>> pool;
    private Integer initializing = 0;
    private long lastQuery = 0;

    private boolean shutdown;

    /**
     * Instantiates a new Object pool.
     *
     * @param min            the min
     * @param idle           the idle
     * @param max            the max
     * @param validationTime the validation time
     * @param idleTimeout    the idle timeout
     */
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
        return new int[]{idle, valid, pool.size(), initializing};
    }

    /**
     * Create object d.
     *
     * @return the d
     */
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

    private void removeObject(ObjectPoolData<?> object, List<ObjectPoolData<?>> objectsToDestroy) {
        pool.remove(object);
        objectsToDestroy.add(object);
        log.info("Removed object : {}", objectsToDestroy);
    }

    private void addObject() {
        synchronized (this.initializing) {
            this.initializing++;
        }
        ObjectPoolData<?> object = createObject();
        synchronized (pool) {
            pool.add(object);
        }
        synchronized (this.initializing) {
            this.initializing--;
        }
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
        List<ObjectPoolData<?>> objectsToDestroy = new ArrayList<>();
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
                                if (System.currentTimeMillis() - lastQuery > 10000) {
                                    removeObject(object, objectsToDestroy);
                                    toDestroy--;
                                }
                            }
                        } else {
                            object.setLastUsed(System.currentTimeMillis());
                        }
                    } else {
                        removeObject(object, objectsToDestroy);
                        toDestroy--;
                    }
                }
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
                        if (object.isIdle() && System.currentTimeMillis() - object.getLastUsed() > idleTime / 2) {
                            if (toDestroy > 0) {
                                removeObject(object, objectsToDestroy);
                                toDestroy--;
                            }
                        } else {
                            object.setLastUsed(System.currentTimeMillis());
                        }
                    } else {
                        removeObject(object, objectsToDestroy);
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
                                removeObject(object, objectsToDestroy);
                                toReduce--;
                            }
                        }
                    } else {
                        removeObject(object, objectsToDestroy);
                        toReduce--;
                    }
                }
            }
        }
        List<Thread> threads = new ArrayList<>();
        log.debug("Objects to destroy : {}", objectsToDestroy.size());
        for (ObjectPoolData<?> object : objectsToDestroy) {
            Thread thread = new Thread(object::destroy);
            thread.start();
            threads.add(thread);
        }
        if (pool.size() + this.initializing < min) {
            int toAdd = min - pool.size() - this.initializing;
            for (int i = 0; i < toAdd; i++) {
                Thread thread = new Thread(this::addObject);
                thread.start();
                threads.add(thread);
            }
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.debug("Object pool metrics : {}", poolSize());
    }

    /**
     * Get object.
     *
     * @return the object
     */
    public Object get(boolean retry) {
        lastQuery = System.currentTimeMillis();
        synchronized (pool) {
            for (ObjectPoolData<?> object : pool) {
                if (object.isValid() && object.isIdle()) {
                    object.setIdle(false);
                    return object.getObject();
                }
            }
        }
        if (!retry && pool.size() + this.initializing < max) {
            Thread thread = new Thread(this::addObject);
            thread.start();
        }
        return null;
    }

    /**
     * Put.
     *
     * @param data the data
     */
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
