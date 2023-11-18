package sd.cloudcomputing.server;

import org.jetbrains.annotations.Nullable;
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

public class ClientConnection extends AbstractConnection<GenericPacket, GenericPacket> {

    private final ClientManager clientManager;
    private final ClientPacketHandler clientPacketHandler;
    private Client client;

    public ClientConnection(Logger logger, Frost frost, ClientManager clientManager, Socket socket, ClientPacketHandler clientPacketHandler) {
        super(GenericPacket.class, GenericPacket.class, logger, frost, socket);
        this.clientManager = clientManager;
        this.clientPacketHandler = clientPacketHandler;
    }


    public boolean start() {
        try {
            Client client = this.acceptLogin();
            if (client != null) {
                this.getLogger().info("Client " + client.getName() + " authenticated successfully");
                this.startReadWrite();
                return true;
            }
        } catch (SerializationException e) {
            getLogger().error("Error deserializing handshake packet: ", e);
        } catch (IOException e) {
            getLogger().error("Error receiving packet to worker: ", e);
        }
        return false;
    }

    private @Nullable Client acceptLogin() throws IOException, SerializationException {
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
        clientPacketHandler.handlePacket(this, packet);
    }

    public Client getClient() {
        return client;
    }

    @Override
    public void onDisconnect() {
        if (client != null) {
            super.getLogger().info("Client " + client.getName() + " disconnected");
        }
    }
}
