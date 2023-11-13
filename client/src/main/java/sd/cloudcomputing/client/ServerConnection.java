package sd.cloudcomputing.client;

import sd.cloudcomputing.common.AbstractConnection;
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

    public ServerConnection(Logger logger, Frost frost, ClientPacketDispatcher scheduler) {
        super(GenericPacket.class, GenericPacket.class, logger, scheduler, frost);
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

    @Override
    public void handlePacket(GenericPacket packet) {
        super.getLogger().info("Received packet " + packet.content().getClass().getSimpleName());
    }

    @Override
    public void onDisconnect() {
        super.getLogger().info("Disconnected from server");
    }
}
