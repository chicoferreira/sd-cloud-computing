package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.Logger;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class WorkerScheduler {

    private final int maxMemoryCapacity;
    private final Logger logger;
    private int currentMemoryUsage;

    private final ReentrantLock lock;
    private final Condition memoryAvailableCondition;

    private final JobExecutor jobExecutor;

    private final BoundedBuffer<JobRequest> queuedJobs;
    private final Thread[] threads;

    private final Consumer<JobResult> endJobCallback;

    private boolean running;

    public WorkerScheduler(Logger logger, JobExecutor jobExecutor, int maxMemoryCapacity, int maxConcurrentJobs, Consumer<JobResult> endJobCallback) {
        this.logger = logger;

        this.maxMemoryCapacity = maxMemoryCapacity;
        this.currentMemoryUsage = 0;
        this.queuedJobs = new BoundedBuffer<>(100);

        this.threads = new Thread[maxConcurrentJobs];

        this.lock = new ReentrantLock();
        this.memoryAvailableCondition = this.lock.newCondition();

        this.jobExecutor = jobExecutor;
        this.endJobCallback = endJobCallback;
    }

    public void queue(JobRequest jobRequest) {
        try {
            queuedJobs.put(jobRequest);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        this.running = false;
        for (Thread thread : threads) {
            try {
                thread.interrupt();
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() {
        this.running = true;
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    while (running) {
                        JobRequest jobRequest = queuedJobs.take(); // this waits until a job is available
                        if (jobRequest == null) {
                            continue;
                        }

                        if (jobRequest.getMemoryNeeded() > this.maxMemoryCapacity) {
                            this.logger.warn("Job " + jobRequest.getJobId() + " requires more memory than the worker can provide");
                            continue;
                        }

                        lock.lock();
                        try {
                            while (!hasMemoryAvailable(jobRequest.getMemoryNeeded())) {
                                memoryAvailableCondition.await();
                            }

                            currentMemoryUsage += jobRequest.getMemoryNeeded();
                            lock.unlock();

                            JobResult jobResult = jobExecutor.execute(jobRequest);
                            this.endJobCallback.accept(jobResult);

                            lock.lock();
                            currentMemoryUsage -= jobRequest.getMemoryNeeded();

                        /* we need to signal all threads because one thread might not
                          have enough memory for its job while another might have */
                            memoryAvailableCondition.signalAll();
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    this.logger.info("Worker thread was interrupted");
                }
            }, "Worker-thread-" + i);
            threads[i].start();
        }
    }

    private boolean hasMemoryAvailable(int memoryNeeded) {
        return currentMemoryUsage + memoryNeeded <= maxMemoryCapacity;
    }

    public int getMaxMemoryCapacity() {
        return maxMemoryCapacity;
    }

    public int getMaxConcurrentJobs() {
        return threads.length;
    }
}
