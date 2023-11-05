package sd.cloudcomputing.client;

import sd.cloudcomputing.common.logging.Logger;

import java.io.IOException;
import java.net.Socket;

public class ServerConnection {

    private final Logger logger;
    private boolean running;
    private Socket socket;

    public ServerConnection(Logger logger) {
        this.logger = logger;
        this.running = false;
    }

    public boolean connect(String ip, int port) {
        try {
            this.socket = new Socket("localhost", 8080);
            logger.info("Connected to server at " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
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

    public boolean isConnected() {
        return running && socket != null && socket.isConnected();
    }
}
