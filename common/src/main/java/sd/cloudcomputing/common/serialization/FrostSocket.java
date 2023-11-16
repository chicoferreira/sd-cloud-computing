package sd.cloudcomputing.common.serialization;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.Socket;

/**
 * A class wrapping a socket with caching of the input and output buffered streams.
 */
public class FrostSocket {

    private final Socket socket;
    private SerializeInput serializeInput;
    private SerializeOutput serializeOutput;

    public FrostSocket(@NotNull Socket socket) {
        this.socket = socket;
    }

    public String getAddressWithPort() {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public SerializeInput readEnd() throws IOException {
        if (serializeInput == null)
            serializeInput = new SerializeInput(new DataInputStream(new BufferedInputStream(socket.getInputStream())));
        return serializeInput;
    }

    public SerializeOutput writeEnd() throws IOException {
        if (serializeOutput == null)
            serializeOutput = new SerializeOutput(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
        return serializeOutput;
    }

    public void close() throws IOException {
        socket.close();
    }

    public boolean isConnected() {
        return socket.isConnected();
    }
}
