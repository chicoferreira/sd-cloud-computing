package sd.cloudcomputing.common.concurrent;

import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedInteger {

    private final ReentrantLock lock = new ReentrantLock();
    private int value;

    public SynchronizedInteger(int startValue) {
        this.value = startValue;
    }

    public int get() {
        return value;
    }

    public int getAndIncrement() {
        lock.lock();
        try {
            return value++;
        } finally {
            lock.unlock();
        }
    }

    public int incrementAndGet() {
        lock.lock();
        try {
            return ++value;
        } finally {
            lock.unlock();
        }
    }

}
