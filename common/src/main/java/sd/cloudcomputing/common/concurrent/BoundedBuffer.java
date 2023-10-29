package sd.cloudcomputing.common.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BoundedBuffer represents a fixed-size buffer that can store elements of type E.
 * The buffer follows the producer-consumer pattern, where multiple threads can
 * put elements into the buffer and multiple threads can take elements from the buffer.
 * The buffer ensures that a put operation blocks when the buffer is full,
 * and a take operation blocks when the buffer is empty.
 *
 * @param <E> the type of elements in the buffer
 */
public class BoundedBuffer<E> {

    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private final Object[] buffer;
    private int putptr, takeptr, count;

    public BoundedBuffer(int capacity) {
        // Array of a generic type cannot be created directly because of type erasure.
        this.buffer = new Object[capacity];
    }

    public void put(E x) throws InterruptedException {
        lock.lock();
        try {
            while (count == buffer.length) {
                notFull.await();
            }
            buffer[putptr] = x;
            if (++putptr == buffer.length) putptr = 0;
            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public E take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            E x = (E) buffer[takeptr];
            if (++takeptr == buffer.length) takeptr = 0;
            --count;
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}