package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.common.logging.Logger;

public class StatusCommand extends AbstractCommand {

    private final Application application;

    public StatusCommand(Application application) {
        super("status", "");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        if (!application.isConnected()) {
            logger.error("Not connected to server");
            return;
        }

        if (application.sendServerStatusRequest()) {
            logger.info("Sent server status request. Response will be printed when received...");
        }
    }
}
