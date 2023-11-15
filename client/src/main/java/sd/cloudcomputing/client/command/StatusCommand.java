package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.client.ServerConnection;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.serialization.SerializationException;

import java.io.IOException;

public class StatusCommand extends AbstractCommand {

    private final Application application;

    public StatusCommand(Application application) {
        super("status", "");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        ServerConnection currentServerConnection = application.getCurrentServerConnection();
        if (currentServerConnection == null) {
            logger.error("Not connected to server");
            return;
        }

        try {
            currentServerConnection.sendServerStatusRequest();
            logger.info("Sent server status request. Response will be printed when received...");
        } catch (IOException e) {
            logger.error("Failed to send server status request: " + e.getMessage());
        } catch (SerializationException e) {
            logger.error("Failed to serialize packet: " + e.getMessage());
        }
    }
}
