package sd.cloudcomputing.common.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class SynchronizedMap<T, V> {

    private final Map<T, V> map;
    private final ReentrantLock lock = new ReentrantLock();

    public SynchronizedMap() {
        this.map = new HashMap<>();
    }

    public V put(T key, V value) {
        lock.lock();
        try {
            return this.map.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public V get(T key) {
        lock.lock();
        try {
            return this.map.get(key);
        } finally {
            lock.unlock();
        }
    }

    public V remove(T key) {
        lock.lock();
        try {
            return this.map.remove(key);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(T key) {
        lock.lock();
        try {
            return this.map.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    public boolean containsValue(V value) {
        lock.lock();
        try {
            return this.map.containsValue(value);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return this.map.size();
        } finally {
            lock.unlock();
        }
    }

    public int sumKeys(Function<T, Integer> function) {
        lock.lock();
        try {
            return this.map.keySet().stream().mapToInt(function::apply).sum();
        } finally {
            lock.unlock();
        }
    }

    public int sumValues(Function<V, Integer> function) {
        lock.lock();
        try {
            return this.map.values().stream().mapToInt(function::apply).sum();
        } finally {
            lock.unlock();
        }
    }


}
