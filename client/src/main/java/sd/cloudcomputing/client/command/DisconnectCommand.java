package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.client.ServerConnection;
import sd.cloudcomputing.common.logging.Logger;

public class DisconnectCommand extends AbstractCommand {

    private final Application application;

    public DisconnectCommand(Application application) {
        super("disconnect", "");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        ServerConnection currentServerConnection = application.getCurrentServerConnection();
        if (currentServerConnection == null) {
            logger.info("Not connected to a server");
            return;
        }

        currentServerConnection.disconnect();
    }
}
