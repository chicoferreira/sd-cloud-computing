package sd.cloudcomputing.server;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
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

public class ClientConnection extends AbstractConnection<GenericPacket, GenericPacket> {

    private final ClientManager clientManager;
    private Client client;

    public ClientConnection(Logger logger, Frost frost, ClientManager clientManager, Socket socket) {
        super(GenericPacket.class, GenericPacket.class, logger, new BoundedBuffer<>(100), frost);
        this.clientManager = clientManager;
        hookSocket(socket);
    }

    public @Nullable Client acceptLogin() throws IOException, SerializationException {
        SerializeInput serializeInput = super.readEnd();

        CSAuthPacket csAuthPacket = super.getFrost().readSerializable(CSAuthPacket.class, serializeInput);
        AuthenticateResult authenticateResult = clientManager.authenticateClient(csAuthPacket.getUsername(), csAuthPacket.getPassword());
        SCAuthResult scAuthResult = new SCAuthResult(authenticateResult);

        SerializeOutput serializeOutput = writeEnd();
        super.getFrost().writeSerializable(scAuthResult, SCAuthResult.class, serializeOutput);
        super.getFrost().flush(serializeOutput);

        if (authenticateResult.isSuccess()) {
            this.client = clientManager.getClient(csAuthPacket.getUsername());
            return this.client;
        }

        return null;
    }

    @Override
    public void handlePacket(GenericPacket packet) {
        super.getLogger().info("Received packet " + packet.content().getClass().getSimpleName());
    }

    @Override
    public void onDisconnect() {
        if (client != null) {
            super.getLogger().info("Client " + client.getName() + " disconnected");
        }
    }
}
