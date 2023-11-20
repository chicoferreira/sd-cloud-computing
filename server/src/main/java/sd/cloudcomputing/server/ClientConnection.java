package sd.cloudcomputing.server;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.JobResult;
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
    private final ClientConnectionManager clientConnectionManager;
    private final ClientPacketHandler clientPacketHandler;
    private Client client;

    public ClientConnection(Logger logger,
                            Frost frost,
                            ClientManager clientManager,
                            Socket socket,
                            ClientConnectionManager clientConnectionManager,
                            ClientPacketHandler clientPacketHandler) {
        super(GenericPacket.class, GenericPacket.class, logger, frost, socket);
        this.clientManager = clientManager;
        this.clientConnectionManager = clientConnectionManager;
        this.clientPacketHandler = clientPacketHandler;
    }


    public void start() {
        Client client = this.acceptLogin();
        if (client != null) {
            this.getLogger().info("Client " + client.getName() + " authenticated successfully");
            this.clientConnectionManager.register(this, client);
            this.startReadWrite();
        }
    }

    private @Nullable Client acceptLogin() {
        try {
            SerializeInput serializeInput = super.readEnd();

            CSAuthPacket csAuthPacket = super.getFrost().readSerializable(CSAuthPacket.class, serializeInput);
            AuthenticateResult authenticateResult = clientManager.authenticateClient(csAuthPacket.getUsername(), csAuthPacket.getPassword());
            SCAuthResult scAuthResult = new SCAuthResult(authenticateResult);

            SerializeOutput serializeOutput = writeEnd();
            super.getFrost().writeSerializable(scAuthResult, SCAuthResult.class, serializeOutput);
            super.getFrost().flush(serializeOutput);

            if (authenticateResult.isSuccess()) {
                return this.client = clientManager.getClient(csAuthPacket.getUsername());
            }
        } catch (IOException | SerializationException e) {
            this.getLogger().warn("Error authenticating client: " + e.getMessage());
        }

        return null;
    }

    @Override
    protected GenericPacket getNextPacketToWrite() throws InterruptedException {
        GenericPacket nextPacketToWrite = super.getNextPacketToWrite();
        if (nextPacketToWrite.content() instanceof JobResult jobResult) {
            super.getLogger().info("Sending job result " + jobResult.getJobId() + " with " + jobResult.getResultType() + " to " + client.getName());
        }
        return nextPacketToWrite;
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
            this.clientConnectionManager.disconnect(client);
        }
    }
}
