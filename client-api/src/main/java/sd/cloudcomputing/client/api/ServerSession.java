package sd.cloudcomputing.client.api;

import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.CSServerStatusRequestPacket;
import sd.cloudcomputing.common.protocol.GenericPacket;
import sd.cloudcomputing.common.protocol.SCServerStatusResponsePacket;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.FrostSocket;
import sd.cloudcomputing.common.serialization.SerializationException;

import java.io.IOException;

public class ServerSession extends AbstractConnection<GenericPacket, GenericPacket> {

    private final ServerSessionListener listener;
    private final ClientJobManager clientJobManager;

    public ServerSession(Logger logger, Frost frost, FrostSocket socket, ServerSessionListener listener, ClientJobManager clientJobManager) {
        super(GenericPacket.class, GenericPacket.class, logger, frost, socket);
        this.listener = listener;
        this.clientJobManager = clientJobManager;
    }

    public void startReadWrite() {
        super.startReadWrite();
    }

    public void scheduleJob(int jobId, byte[] bytes, int memory) {
        JobRequest jobRequest = new JobRequest(jobId, bytes, memory);
        super.enqueuePacket(new GenericPacket(JobRequest.PACKET_ID, jobRequest));
    }

    public void sendServerStatusRequest() throws IOException, SerializationException {
        GenericPacket packet = new GenericPacket(CSServerStatusRequestPacket.PACKET_ID, new CSServerStatusRequestPacket());
        this.getSocket().writeFlush(super.getFrost(), packet, GenericPacket.class);
    }

    @Override
    public void handlePacket(GenericPacket packet) {
        switch (packet.id()) {
            case SCServerStatusResponsePacket.PACKET_ID:
                SCServerStatusResponsePacket responsePacket = (SCServerStatusResponsePacket) packet.content();
                listener.onServerStatusResponse(this, responsePacket);
                break;
            case JobResult.PACKET_ID:
                JobResult jobResult = (JobResult) packet.content();
                ClientJob.Received received = clientJobManager.registerJobResult(jobResult.jobId(), jobResult);
                listener.onJobResultResponse(this, received);
                break;
            default:
                getLogger().warn("Unknown packet (id=" + packet.id() + "): " + packet.content());
                break;
        }
    }

    @Override
    protected void onDisconnect() {
        listener.onDisconnect(this);
    }
}
