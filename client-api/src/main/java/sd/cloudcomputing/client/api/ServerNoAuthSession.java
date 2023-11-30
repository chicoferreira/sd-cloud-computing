package sd.cloudcomputing.client.api;

import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.CSAuthPacket;
import sd.cloudcomputing.common.protocol.SCAuthResult;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.FrostSocket;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.io.IOException;
import java.net.Socket;

public class ServerNoAuthSession {

    private final FrostSocket socket;
    private final Frost frost;

    public ServerNoAuthSession(Socket socket, Frost frost) {
        this.socket = new FrostSocket(socket);
        this.frost = frost;
    }

    public AuthenticateResult login(String username, String password) throws SerializationException, IOException {
        if (!socket.isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }

        CSAuthPacket authPacket = new CSAuthPacket(username, password);

        socket.writeFlush(this.frost, authPacket, CSAuthPacket.class);
        SCAuthResult scAuthResult = socket.read(frost, SCAuthResult.class);

        return scAuthResult.result();
    }

    public ServerSession createLoggedSession(Logger logger, Client client, ServerSessionListener listener) {
        // This assumes the socket is already connected and authenticated
        return new ServerSession(logger, frost, socket, listener, client.getJobManager());
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    public String getAddressWithPort() {
        return socket.getAddressWithPort();
    }
}
