package com.ticker.common.util.objectpool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ObjectPool<D extends ObjectPoolData<?>> {
    private final int min;
    private final int idle;
    private final int max;
    private final long validationTime;
    private final long idleTime;
    private final Set<ObjectPoolData<?>> pool;

    public ObjectPool(int min, int idle, int max, long validationTime, long idleTimeout) {
        this.min = min;
        this.idle = idle;
        this.max = max;
        this.validationTime = validationTime;
        this.idleTime = idleTimeout;

        pool = Collections.newSetFromMap(new ConcurrentHashMap<>());

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this::validate, 0, validationTime, TimeUnit.MILLISECONDS);
    }

    private void poolSize() {
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
//        log.info("Total:\t" + pool.size() + "\tIdle:\t" + idle + "\tValid:\t" + valid + "\tInvalid:\t" + invalid);
    }

    public abstract D createObject();

    private void removeObject(ObjectPoolData<?> object) {
        pool.remove(object);
        Thread thread = new Thread(() -> object.destroy());
        thread.start();
    }

    private void validate() {
        for (ObjectPoolData<?> object : pool) {
            if (object.isValid() && !object.isIdle()) {
                object.setLastUsed(System.currentTimeMillis());
            }
        }
        if (pool.size() > idle) {
            int toDestroy = pool.size() - idle;
            for (ObjectPoolData<?> object : pool) {
                if (toDestroy == 0) {
                    break;
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
        if (pool.size() > min) {
            int toReduce = pool.size() - min;
            for (ObjectPoolData<?> object : pool) {
                if (toReduce == 0) {
                    break;
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
                Thread.sleep(validationTime);
            } catch (InterruptedException ignored) {
            }
        } while (true);
    }

    public void put(Object data) {
        for (ObjectPoolData<?> object : pool) {
            if (object.equalsObject(data)) {
                object.setIdle(true);
                break;
            }
        }
    }

}
