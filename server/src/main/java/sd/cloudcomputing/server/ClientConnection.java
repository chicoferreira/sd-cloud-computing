package sd.cloudcomputing.server;

import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.CSAuthPacket;
import sd.cloudcomputing.common.protocol.SCAuthResult;
import sd.cloudcomputing.common.serialization.*;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.io.IOException;
import java.net.Socket;

public class ClientConnection {

    private final Logger logger;
    private final FrostSocket frostSocket;
    private final Frost frost;
    private final ClientManager clientManager;
    private boolean running;
    private Thread readThread;
    private Thread writeThread;
    private Client client;

    public ClientConnection(Logger logger, Socket socket, Frost frost, ClientManager clientManager) {
        this.logger = logger;
        this.frostSocket = new FrostSocket(socket);
        this.frost = frost;
        this.clientManager = clientManager;
    }

    public SerializeInput readEnd() throws IOException {
        return this.frostSocket.readEnd();
    }

    public SerializeOutput writeEnd() throws IOException {
        return this.frostSocket.writeEnd();
    }

    public void close() throws IOException {
        this.frostSocket.close();
    }

    public void run() {
        this.running = true;

        try {
            SerializeInput serializeInput = readEnd();

            CSAuthPacket csAuthPacket = frost.readSerializable(CSAuthPacket.class, serializeInput);
            AuthenticateResult authenticateResult = clientManager.authenticateClient(csAuthPacket.getUsername(), csAuthPacket.getPassword());
            SCAuthResult scAuthResult = new SCAuthResult(authenticateResult);

            SerializeOutput serializeOutput = writeEnd();
            frost.writeSerializable(scAuthResult, SCAuthResult.class, serializeOutput);
            frost.flush(serializeOutput);

            if (scAuthResult.isSuccess()) {
                logger.info("Client " + csAuthPacket.getUsername() + " successfully authenticated");

                this.client = clientManager.getClient(csAuthPacket.getUsername());

                this.readThread = new Thread(this::runRead, "Client-Read-Thread");
                this.writeThread = new Thread(this::runWrite, "Client-Write-Thread");

                this.readThread.start();
                this.writeThread.start();
            } else {
                logger.info("Client " + csAuthPacket.getUsername() + " failed to authenticate");
                close();
            }
        } catch (IOException e) {
            logger.error("Error while reading from client socket: " + e.getMessage());
        } catch (SerializationException e) {
            logger.error("Error while deserializing packet: " + e.getMessage());
        }
    }

    public void runRead() {
        this.running = true;

        try {
            while (running) {
                SerializeInput serializeInput = readEnd();
                int i = frost.readInt(serializeInput); // block reading
                logger.info("Received " + i + " bytes from client");
            }
        } catch (IOException e) {
            logger.error("Error while reading from client socket: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        logger.info("Client " + client.getName() + " disconnected");
        this.running = false;
        try {
            close();
        } catch (IOException e) {
            logger.error("Failed to close client socket", e);
        }
    }

    public void runWrite() {
    }

}
