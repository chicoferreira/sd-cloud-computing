package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.WorkerJobResult;
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

    private final Consumer<WorkerJobResult> endJobCallback;

    private boolean running;

    public WorkerScheduler(Logger logger, JobExecutor jobExecutor, int maxMemoryCapacity, int maxConcurrentJobs, Consumer<WorkerJobResult> endJobCallback) {
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
            if (thread != null) {
                thread.interrupt();
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

                        if (jobRequest.memoryNeeded() > this.maxMemoryCapacity) {
                            this.logger.warn("Job " + jobRequest.jobId() + " requires more memory than the worker can provide");
                            continue;
                        }

                        lock.lock();
                        try {
                            while (!hasMemoryAvailable(jobRequest.memoryNeeded())) {
                                memoryAvailableCondition.await();
                            }

                            currentMemoryUsage += jobRequest.memoryNeeded();
                            lock.unlock();

                            WorkerJobResult jobResult = jobExecutor.execute(jobRequest);

                            switch (jobResult) {
                                case WorkerJobResult.Success success ->
                                        this.logger.info("Job " + success.jobId() + " succeeded with result: " + success.data().length + " bytes");
                                case WorkerJobResult.Failure failure ->
                                        this.logger.info("Job " + failure.jobId() + " failed with error code " + failure.errorCode() + ": " + failure.errorMessage());
                            }

                            this.endJobCallback.accept(jobResult);

                            lock.lock();
                            currentMemoryUsage -= jobRequest.memoryNeeded();

                        /* we need to signal all threads because one thread might not
                          have enough memory for its job while another might have */
                            memoryAvailableCondition.signalAll();
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException ignored) {
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
