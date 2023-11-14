package sd.cloudcomputing.common.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public class SynchronizedList<T> {

    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayList<T> arrayList;

    public SynchronizedList() {
        this.arrayList = new ArrayList<>();
    }

    public boolean add(T t) {
        lock.lock();
        try {
            return this.arrayList.add(t);
        } finally {
            lock.unlock();
        }
    }

    public T get(int index) {
        lock.lock();
        try {
            return this.arrayList.get(index);
        } finally {
            lock.unlock();
        }
    }

    public T remove(int index) {
        lock.lock();
        try {
            return this.arrayList.remove(index);
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(T t) {
        lock.lock();
        try {
            return this.arrayList.remove(t);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return this.arrayList.size();
        } finally {
            lock.unlock();
        }
    }

    public void forEach(Consumer<T> consumer) {
        lock.lock();
        try {
            this.arrayList.forEach(consumer);
        } finally {
            lock.unlock();
        }
    }

    public int sum(ToIntFunction<T> toIntFunction) {
        lock.lock();
        try {
            return this.arrayList.stream().mapToInt(toIntFunction).sum();
        } finally {
            lock.unlock();
        }
    }

    public int max(ToIntFunction<T> toIntFunction) {
        lock.lock();
        try {
            return this.arrayList.stream().mapToInt(toIntFunction).max().orElse(0);
        } finally {
            lock.unlock();
        }
    }

    public void internalLock() {
        lock.lock();
    }

    public void internalUnlock() {
        lock.unlock();
    }

    public List<T> getInternalList() {
        return this.arrayList;
    }
}
