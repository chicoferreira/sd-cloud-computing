package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.WSHandshakePacket;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;
import java.net.Socket;

public class ServerConnection extends AbstractConnection<JobResult, JobRequest> {

    private final WorkerScheduler workerScheduler;
    private final Worker worker;

    public ServerConnection(Logger logger, Frost frost, Socket socket, WorkerScheduler workerScheduler, Worker worker) {
        super(JobResult.class, JobRequest.class, logger, frost, socket);
        this.workerScheduler = workerScheduler;
        this.worker = worker;
    }

    @Override
    protected void handlePacket(JobRequest packet) {
        getLogger().info("Received job request with id " + packet.jobId() + " and " + packet.data().length + " bytes of data");
        workerScheduler.queue(packet);
    }

    @Override
    protected void onDisconnect() {
        getLogger().info("Disconnected from server");
        worker.stop();
    }

    public void start() {
        try {
            SerializeOutput output = writeEnd();
            WSHandshakePacket packet = new WSHandshakePacket(this.workerScheduler.getMaxMemoryCapacity());

            getFrost().writeSerializable(packet, WSHandshakePacket.class, output);
            getFrost().flush(output);

            startReadWrite();
        } catch (IOException | SerializationException e) {
            getLogger().error("Error sending handshake packet: ", e);
        }
    }
}
