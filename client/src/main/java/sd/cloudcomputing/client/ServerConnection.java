package sd.cloudcomputing.client;

import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.serialization.FrostSocket;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;
import java.net.Socket;

public class ServerConnection {

    private final Logger logger;
    private boolean running;
    private FrostSocket socket;

    public ServerConnection(Logger logger) {
        this.logger = logger;
        this.running = false;
    }

    public boolean connect(String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            logger.info("Connected to server at " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            this.socket = new FrostSocket(socket);
            return true;
        } catch (Exception e) {
            logger.error("Error connecting to server: " + e.getMessage());
            return false;
        }
    }

    public void run() {
        this.running = true;

        logger.info("Disconnecting after 5 seconds");
        try {
            Thread.sleep(5000);
            socket.close();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Disconnected");
        this.running = false;
    }

    public void enqueue(String job) {
        logger.info("Enqueued job: " + job);
    }

    public SerializeInput readEnd() throws IOException {
        return socket.readEnd();
    }

    public SerializeOutput writeEnd() throws IOException {
        return socket.writeEnd();
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected();
    }

    public void close() throws IOException {
        socket.close();
    }
}
