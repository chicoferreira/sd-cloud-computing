package sd.cloudcomputing.server;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.concurrent.SynchronizedList;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;

import java.util.Comparator;
import java.util.List;

public class OvertakingJobSchedulerImpl implements JobScheduler {

    private static final int MAX_OVERTAKES = 5;

    private final SynchronizedMap<Integer, QueuedJobRequest> queuedJobRequests;

    private final ConnectedWorkerManager connectedWorkerManager;
    private final Server server;

    public OvertakingJobSchedulerImpl(ConnectedWorkerManager connectedWorkerManager, Server server) {
        this.connectedWorkerManager = connectedWorkerManager;
        this.server = server;
        this.queuedJobRequests = new SynchronizedMap<>();
    }

    @Override
    public boolean scheduleJob(JobRequest serverJobRequest) {
        SynchronizedList<WorkerConnection> workerConnections = connectedWorkerManager.getWorkerConnections();
        workerConnections.internalLock();

        List<WorkerConnection> internalList = workerConnections.getInternalList();
        try {
            WorkerConnection workerConnection = getWorkerWithFreeMemory(serverJobRequest.memoryNeeded(), internalList);

            QueuedJobRequest queuedJobRequest = null;
            if (workerConnection == null) { // no worker has enough current free memory for the job
                queuedJobRequest = this.queuedJobRequests.get(serverJobRequest.jobId());

                if (queuedJobRequest == null) { // job has not been overtaken yet
                    queuedJobRequest = new QueuedJobRequest(serverJobRequest);
                    this.queuedJobRequests.put(serverJobRequest.jobId(), queuedJobRequest);
                }

                int numberOfOvertakes = queuedJobRequest.getNumberOfOvertakes();

                if (numberOfOvertakes <= MAX_OVERTAKES) {
                    // job has no place to run right now,
                    // but it will be rescheduled later if there are connected workers with enough capacity
                    return getWorkerMaxCapacity(internalList) >= serverJobRequest.memoryNeeded();
                } else { // job has been overtaken too many times, schedule now!
                    workerConnection = getWorkerWithMostFreeMemory(serverJobRequest.memoryNeeded(), internalList);
                    if (workerConnection == null) { // no worker has enough memory for the job
                        this.queuedJobRequests.remove(serverJobRequest.jobId()); // job won't be scheduled, no need to keep track of it anymore
                        return false;
                    }
                }
            }

            this.queuedJobRequests.remove(serverJobRequest.jobId()); // job has been scheduled no need to keep track of it anymore

            for (QueuedJobRequest otherQueuedJobRequest : this.queuedJobRequests.values()) {
                if (queuedJobRequest == null || otherQueuedJobRequest.getNanoTimeWhenFirstQueued() < queuedJobRequest.getNanoTimeWhenFirstQueued()) {
                    otherQueuedJobRequest.incrementNumberOfOvertakes(); // increment all jobs that have been queued before this one
                }
            }

            workerConnection.enqueuePacket(serverJobRequest);
        } finally {
            workerConnections.internalUnlock();
        }
        return true;
    }

    public void notifyReschedule() { // executed when a worker finishes a job or disconnects
        for (QueuedJobRequest queuedJobRequest : this.queuedJobRequests.values()) {
            // reschedule all jobs so they can maybe be scheduled on a worker with more free memory
            server.rescheduleJob(queuedJobRequest.getJobRequest());
        }
    }

    /**
     * @return the worker with the most free memory which has enough current free memory for the job
     */
    @Nullable
    private WorkerConnection getWorkerWithFreeMemory(int memoryNeeded, List<WorkerConnection> workerConnections) {
        return workerConnections.stream()
                .filter(worker -> worker.getEstimatedFreeMemory() >= memoryNeeded)
                .max(Comparator.comparingInt(WorkerConnection::getEstimatedFreeMemory))
                .orElse(null);
    }

    /**
     * @return the worker with the most free memory which has enough memory capacity for the job
     */
    @Nullable
    private WorkerConnection getWorkerWithMostFreeMemory(int memoryNeeded, List<WorkerConnection> workerConnections) {
        return workerConnections.stream()
                .filter(worker -> worker.getMaxMemoryCapacity() >= memoryNeeded)
                .max(Comparator.comparingInt(WorkerConnection::getEstimatedFreeMemory))
                .orElse(null);
    }

    private int getWorkerMaxCapacity(List<WorkerConnection> workerConnections) {
        return workerConnections.stream()
                .mapToInt(WorkerConnection::getMaxMemoryCapacity)
                .max()
                .orElse(0);
    }

    static class QueuedJobRequest {
        private final JobRequest jobRequest;
        private final long nanoTimeWhenFirstQueued;
        private int numberOfOvertakes;

        public QueuedJobRequest(JobRequest jobRequest) {
            this.jobRequest = jobRequest;
            this.nanoTimeWhenFirstQueued = System.nanoTime();
            this.numberOfOvertakes = 0;
        }

        public JobRequest getJobRequest() {
            return jobRequest;
        }

        public long getNanoTimeWhenFirstQueued() {
            return nanoTimeWhenFirstQueued;
        }

        public int getNumberOfOvertakes() {
            return numberOfOvertakes;
        }

        public void incrementNumberOfOvertakes() {
            this.numberOfOvertakes++;
        }
    }
}
