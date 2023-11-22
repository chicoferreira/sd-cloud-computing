package sd.cloudcomputing.server;

import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.WSHandshakePacket;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;

import java.io.IOException;
import java.net.Socket;

public class WorkerConnection extends AbstractConnection<JobRequest, JobResult> {

    private final Server server;
    private final SynchronizedMap<Integer, JobRequest> pendingJobRequests;
    private final ConnectedWorkerManager connectedWorkerManager;
    private int maxMemoryCapacity;

    public WorkerConnection(Logger logger, Frost frost, Socket socket, Server server, ConnectedWorkerManager connectedWorkerManager) {
        super(JobRequest.class, JobResult.class, logger, frost, socket);
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
        return new Status(this.pendingJobRequests.size(), this.maxMemoryCapacity, this.pendingJobRequests.sumValues(JobRequest::memoryNeeded));
    }

    @Override
    public void handlePacket(JobResult jobResult) {
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
        super.getLogger().info("Sending job request with id " + packet.jobId() + " and " + packet.data().length + " bytes of data");
        return super.mapPacketBeforeSend(packet);
    }

    private void performHandshake() throws SerializationException, IOException {
        SerializeInput input = readEnd();
        WSHandshakePacket wsHandshakePacket = super.getFrost().readSerializable(WSHandshakePacket.class, input);
        super.getLogger().info("Worker connected with max memory capacity of " + wsHandshakePacket.maxMemoryCapacity());

        this.maxMemoryCapacity = wsHandshakePacket.maxMemoryCapacity();
    }

    @Override
    public void onDisconnect() {
        super.getLogger().info("Worker disconnected");
        connectedWorkerManager.notifyDisconnect(this);
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
