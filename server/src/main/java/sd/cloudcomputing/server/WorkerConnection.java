package sd.cloudcomputing.server;

import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.WorkerJobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.WSHandshakePacket;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class WorkerConnection extends AbstractConnection<JobRequest, WorkerJobResult> {

    private final Server server;
    private final SynchronizedMap<Integer, JobRequest> pendingJobRequests;
    private final ConnectedWorkerManager connectedWorkerManager;
    private int maxMemoryCapacity;

    public WorkerConnection(Logger logger, Frost frost, Socket socket, Server server, ConnectedWorkerManager connectedWorkerManager) {
        super(JobRequest.class, WorkerJobResult.class, logger, frost, socket);
        this.server = server;
        this.connectedWorkerManager = connectedWorkerManager;
        this.pendingJobRequests = new SynchronizedMap<>();
    }

    public int getEstimatedFreeMemory() {
        return this.maxMemoryCapacity - this.pendingJobRequests.sumValues(JobRequest::memoryNeeded);
    }

    public int getMaxMemoryCapacity() {
        return this.maxMemoryCapacity;
    }

    public Status getStatus() {
        this.pendingJobRequests.internalLock();
        try {
            Map<Integer, JobRequest> internalMap = this.pendingJobRequests.getInternalDelegate();
            return new Status(internalMap.size(), this.maxMemoryCapacity, internalMap.values().stream().mapToInt(JobRequest::memoryNeeded).sum());
        } finally {
            this.pendingJobRequests.internalUnlock();
        }
    }

    @Override
    public void handlePacket(WorkerJobResult workerJobResult) {
        JobResult jobResult = workerJobResult.toJobResult();
        super.getLogger().info("Received job result " + jobResult.jobId() + " with " + jobResult.resultType());

        JobRequest jobRequest = pendingJobRequests.remove(jobResult.jobId());
        if (jobRequest == null) {
            super.getLogger().warn("Not pending job request: " + jobResult.jobId() + ". Race condition?");
        }

        server.queueJobResultToClient(jobResult);
    }

    public record Status(int jobsRunning, int memoryCapacity, int currentEstimatedMemoryUsage) {
    }

    @Override
    public void enqueuePacket(JobRequest packet) {
        this.pendingJobRequests.put(packet.jobId(), packet);
        super.enqueuePacket(packet);
    }

    @Override
    protected JobRequest mapPacketBeforeSend(JobRequest packet) {
        return super.mapPacketBeforeSend(packet);
    }

    private void performHandshake() throws SerializationException, IOException {
        super.getLogger().info("Performing handshake with worker...");
        SerializeInput input = readEnd();
        WSHandshakePacket wsHandshakePacket = super.getFrost().readSerializable(WSHandshakePacket.class, input);
        super.getLogger().info("Worker connected with max memory capacity of " + wsHandshakePacket.maxMemoryCapacity());

        this.maxMemoryCapacity = wsHandshakePacket.maxMemoryCapacity();
    }

    @Override
    public void onDisconnect() {
        super.getLogger().info("Worker disconnected");
        connectedWorkerManager.notifyDisconnect(this);

        server.handleWorkerDisconnect(this.pendingJobRequests.values());
    }

    public boolean start() {
        try {
            this.performHandshake();
            this.startReadWrite();
            return true;
        } catch (SerializationException e) {
            getLogger().error("Error deserializing handshake packet: ", e);
        } catch (IOException e) {
            getLogger().error("Error receiving packet to worker: ", e);
        }
        return false;
    }
}
