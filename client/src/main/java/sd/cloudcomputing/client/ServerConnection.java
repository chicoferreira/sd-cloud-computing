package sd.cloudcomputing.client;

import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.concurrent.SynchronizedInteger;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.CSAuthPacket;
import sd.cloudcomputing.common.protocol.GenericPacket;
import sd.cloudcomputing.common.protocol.SCAuthResult;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.io.IOException;
import java.net.Socket;

public class ServerConnection extends AbstractConnection<GenericPacket, GenericPacket> {

    private final SynchronizedInteger currentJobId = new SynchronizedInteger(0);

    public ServerConnection(Logger logger, Frost frost) {
        super(GenericPacket.class, GenericPacket.class, logger, frost);
    }

    public boolean connect(String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            super.hookSocket(socket);

            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthenticateResult login(String username, String password) throws IOException, SerializationException {
        CSAuthPacket authPacket = new CSAuthPacket(username, password);

        SerializeOutput serializeOutput = super.writeEnd();

        super.getFrost().writeSerializable(authPacket, CSAuthPacket.class, serializeOutput);
        super.getFrost().flush(serializeOutput);

        SerializeInput serializeInput = super.readEnd();

        SCAuthResult scAuthResult = super.getFrost().readSerializable(SCAuthResult.class, serializeInput);

        return scAuthResult.result();
    }

    public int scheduleJob(byte[] bytes, int memory) {
        int jobId = currentJobId.getAndIncrement();

        JobRequest jobRequest = new JobRequest(jobId, bytes, memory);
        super.enqueuePacket(new GenericPacket(JobRequest.PACKET_ID, jobRequest)); // TODO: refactor this api

        return jobId;
    }

    @Override
    public void handlePacket(GenericPacket packet) {
        super.getLogger().info("Received packet " + packet.content().getClass().getSimpleName());
    }

    @Override
    public void onDisconnect() {
        super.getLogger().info("Disconnected from server");
    }
}
