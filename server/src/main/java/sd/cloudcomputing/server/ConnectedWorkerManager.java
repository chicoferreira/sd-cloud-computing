package sd.cloudcomputing.server;

import sd.cloudcomputing.common.concurrent.SynchronizedList;
import sd.cloudcomputing.common.protocol.SCServerStatusResponsePacket;

import java.util.List;

public class ConnectedWorkerManager {

    private final SynchronizedList<WorkerConnection> workerConnections;

    public ConnectedWorkerManager() {
        this.workerConnections = new SynchronizedList<>();
    }

    public SynchronizedList<WorkerConnection> getWorkerConnections() {
        return workerConnections;
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

    public SCServerStatusResponsePacket getServerStatus() {
        List<WorkerConnection.Status> workerStatuses = workerConnections.map(WorkerConnection::getStatus);

        int totalCapacity = workerStatuses.stream().mapToInt(WorkerConnection.Status::memoryCapacity).sum();
        int totalMemoryUsage = workerStatuses.stream().mapToInt(WorkerConnection.Status::currentEstimatedMemoryUsage).sum();
        int memoryUsagePercentage = (int) (((double) totalMemoryUsage / (double) totalCapacity) * 100);

        return new SCServerStatusResponsePacket(
                workerStatuses.size(),
                totalCapacity,
                workerStatuses.stream().mapToInt(WorkerConnection.Status::memoryCapacity).max().orElse(0),
                memoryUsagePercentage,
                workerStatuses.stream().mapToInt(WorkerConnection.Status::jobsRunning).sum()
        );
    }
}
