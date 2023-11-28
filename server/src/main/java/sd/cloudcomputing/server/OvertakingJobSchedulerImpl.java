package sd.cloudcomputing.server;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.concurrent.SynchronizedList;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;
import sd.cloudcomputing.common.logging.Logger;

import java.util.*;

public class OvertakingJobSchedulerImpl implements JobScheduler {

    private static final int MAX_OVERTAKES = 50;

    private final SynchronizedMap<Integer, QueuedJobRequest> queuedJobRequests;

    private final ConnectedWorkerManager connectedWorkerManager;
    private final Logger logger;

    public OvertakingJobSchedulerImpl(ConnectedWorkerManager connectedWorkerManager, Logger logger) {
        this.connectedWorkerManager = connectedWorkerManager;
        this.logger = logger;
        this.queuedJobRequests = new SynchronizedMap<>(new LinkedHashMap<>()); // IMPORTANT: LinkedHashMap to preserve insertion order
    }

    @Override
    public boolean scheduleJob(JobRequest serverJobRequest) {
        this.queuedJobRequests.internalLock();
        SynchronizedList<WorkerConnection> workerConnections = connectedWorkerManager.getWorkerConnections();
        workerConnections.internalLock();

        try {
            return scheduleJobInternal(serverJobRequest,
                    workerConnections.getInternalList(),
                    this.queuedJobRequests.getInternalDelegate());
        } finally {
            workerConnections.internalUnlock();
            this.queuedJobRequests.internalUnlock();
        }
    }

    public void notifyReschedule() { // executed when a worker changes memory state (e.g., disconnects or finishes a job)
        this.queuedJobRequests.internalLock();
        try {
            Map<Integer, QueuedJobRequest> internalQueuedJobRequests = this.queuedJobRequests.getInternalDelegate();
            List<QueuedJobRequest> values = new ArrayList<>(internalQueuedJobRequests.values()); // copy to avoid concurrent modification exception

            for (QueuedJobRequest queuedJobRequest : values) { // queuedJobRequests is a LinkedHashMap, so this will iterate in insertion order
                // reschedule all jobs, so they can maybe be scheduled on a worker with more free memory
                scheduleJobInternal(queuedJobRequest.getJobRequest(),
                        connectedWorkerManager.getWorkerConnections().getInternalList(),
                        internalQueuedJobRequests);
            }
        } finally {
            this.queuedJobRequests.internalUnlock();
        }
    }

    private boolean scheduleJobInternal(JobRequest serverJobRequest, List<WorkerConnection> workerConnectionsInternalList, Map<Integer, QueuedJobRequest> internalQueuedJobRequests) {
        // Assumes the map is already locked
        boolean overtaking = false;
        WorkerConnection workerConnection = getWorkerWithFreeMemory(serverJobRequest.memoryNeeded(), workerConnectionsInternalList);

        QueuedJobRequest queuedJobRequest = null;
        if (workerConnection == null) { // no worker has enough current free memory for the job
            queuedJobRequest = internalQueuedJobRequests.get(serverJobRequest.jobId());

            if (queuedJobRequest == null) { // job has not been overtaken yet
                queuedJobRequest = new QueuedJobRequest(serverJobRequest);
                internalQueuedJobRequests.put(serverJobRequest.jobId(), queuedJobRequest);
            }

            int numberOfOvertakes = queuedJobRequest.getNumberOfOvertakes();

            workerConnection = getWorkerWithMostFreeMemory(serverJobRequest.memoryNeeded(), workerConnectionsInternalList);
            if (workerConnection == null) { // no worker has enough memory capacity for the job
                internalQueuedJobRequests.remove(serverJobRequest.jobId()); // job won't be scheduled, no need to keep track of it anymore
                return false;
            }

            if (numberOfOvertakes <= MAX_OVERTAKES) {
                return true;
            }

            overtaking = true;
        }

        internalQueuedJobRequests.remove(serverJobRequest.jobId()); // job has been scheduled no need to keep track of it anymore

        for (QueuedJobRequest otherQueuedJobRequest : internalQueuedJobRequests.values()) {
            if (queuedJobRequest == null || otherQueuedJobRequest.getNanoTimeWhenFirstQueued() < queuedJobRequest.getNanoTimeWhenFirstQueued()) {
                otherQueuedJobRequest.incrementNumberOfOvertakes(); // increment all jobs that have been queued before this one
            }
        }

        if (overtaking) {
            logger.info("(OVERTAKING) Enqueuing job request with server id " + serverJobRequest.jobId() + " and " + serverJobRequest.memoryNeeded());
        } else {
            logger.info("Enqueuing job request with server id " + serverJobRequest.jobId() + " and " + serverJobRequest.data().length + " bytes of data");
        }

        workerConnection.enqueuePacket(serverJobRequest);
        return true;
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
