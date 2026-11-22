package com.rupeek.hotel.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * One reentrant lock per key. Used to serialise work on the same booking / room type
 * without blocking unrelated ones. The map is never trimmed - fine for an in-memory
 * service, would need eviction (or a striped lock) in production.
 */
public final class KeyedLocks {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T supply(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void run(String key, Runnable action) {
        supply(key, () -> {
            action.run();
            return null;
        });
    }
}
