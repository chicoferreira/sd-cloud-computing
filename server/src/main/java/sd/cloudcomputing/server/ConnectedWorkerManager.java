package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.concurrent.SynchronizedList;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ConnectedWorkerManager {

    private final SynchronizedList<WorkerConnection> workerConnections;

    public ConnectedWorkerManager() {
        this.workerConnections = new SynchronizedList<>();
    }

    public boolean scheduleJob(JobRequest jobRequest) {
        WorkerConnection workerConnection = findAvailableWorker(jobRequest.memoryNeeded());
        if (workerConnection == null) {
            return false;
        }

        workerConnection.enqueuePacket(jobRequest);
        return true;
    }

    public SynchronizedList<WorkerConnection> getWorkerConnections() {
        return workerConnections;
    }

    public WorkerConnection findAvailableWorker(int memoryNeeded) {
        workerConnections.internalLock();
        try {
            List<WorkerConnection> internalList = workerConnections.getInternalList();
            Optional<WorkerConnection> first = internalList.stream()
                    .sorted(Comparator.comparingInt(WorkerConnection::getEstimatedFreeMemory))
                    .filter(workerConnection -> workerConnection.getMaxMemoryCapacity() >= memoryNeeded)
                    .findFirst();

            return first.orElse(null);
        } finally {
            workerConnections.internalUnlock();
        }
    }

    public void addConnectedWorker(WorkerConnection workerConnection) {
        workerConnections.add(workerConnection);
    }

    public void closeAll() {
        workerConnections.forEach(WorkerConnection::disconnect);
    }

    public void notifyDisconnect(WorkerConnection workerConnection) {
        workerConnections.remove(workerConnection);
    }

    public int getTotalConnectedWorkers() {
        return workerConnections.size();
    }

    public int getTotalMemoryCombined() {
        return workerConnections.sum(WorkerConnection::getMaxMemoryCapacity);
    }

    public int getMaxMemory() {
        return workerConnections.max(WorkerConnection::getMaxMemoryCapacity);
    }

    public int getMemoryUsagePercentage() {
        return (int) (((double) getTotalMemoryCombined() / (double) getMaxMemory()) * 100);
    }

    public int getAmountOfJobsCurrentlyRunning() {
        return workerConnections.sum(WorkerConnection::getAmountOfJobsRunning);
    }
}
