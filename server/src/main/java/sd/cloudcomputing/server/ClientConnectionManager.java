package sd.cloudcomputing.server;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;

public class ClientConnectionManager {

    private final SynchronizedMap<Client, ClientConnection> clientConnectionMap;

    public ClientConnectionManager() {
        this.clientConnectionMap = new SynchronizedMap<>();
    }

    public void register(ClientConnection clientConnection, Client client) {
        clientConnectionMap.put(client, clientConnection);
    }

    public void disconnect(Client client) {
        clientConnectionMap.remove(client);
    }

    public @Nullable("when client is not connected") ClientConnection getClientConnection(Client client) {
        return clientConnectionMap.get(client);
    }

    public void disconnectAll() {
        clientConnectionMap.forEach((client, clientConnection) -> clientConnection.disconnect());
    }
}
