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

    private final SynchronizedMap<Integer, JobRequest> pendingJobRequests;
    private final ConnectedWorkerManager connectedWorkerManager;
    private int maxMemoryCapacity;

    public WorkerConnection(Logger logger, Frost frost, Socket socket, ConnectedWorkerManager connectedWorkerManager) {
        super(JobRequest.class, JobResult.class, logger, frost, socket);
        this.connectedWorkerManager = connectedWorkerManager;
        this.pendingJobRequests = new SynchronizedMap<>();
    }

    public int getMaxMemoryCapacity() {
        return maxMemoryCapacity;
    }

    public int getCurrentEstimatedMemoryUsage() {
        return this.pendingJobRequests.sumValues(JobRequest::memoryNeeded);
    }

    public int getEstimatedFreeMemory() {
        return this.maxMemoryCapacity - getCurrentEstimatedMemoryUsage();
    }

    @Override
    public void handlePacket(JobResult jobResult) {
        super.getLogger().info("Received job result: " + jobResult.getJobId() + " " + new String(jobResult.getData()));

        JobRequest jobRequest = pendingJobRequests.remove(jobResult.getJobId());
        if (jobRequest == null) {
            super.getLogger().warn("Not pending job request: " + jobResult.getJobId() + ". Race condition?");
        }
    }

    @Override
    public JobRequest getNextPacketToWrite() throws InterruptedException {
        JobRequest jobRequest = super.getNextPacketToWrite();
        pendingJobRequests.put(jobRequest.jobId(), jobRequest);

        super.getLogger().info("Sending job request with id " + jobRequest.jobId() + " and " + jobRequest.data().length + " bytes of data");
        return jobRequest;
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

    public int getAmountOfJobsRunning() {
        return pendingJobRequests.size();
    }

    public boolean start() {
        try {
            this.performHandshake();
            this.startReadWrite();
        } catch (SerializationException e) {
            getLogger().error("Error deserializing handshake packet: ", e);
        } catch (IOException e) {
            getLogger().error("Error receiving packet to worker: ", e);
        }
        return false;
    }
}
