package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.common.logging.Logger;

public class DisconnectCommand extends AbstractCommand {

    private final Application application;

    public DisconnectCommand(Application application) {
        super("disconnect", "");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        if (!application.isConnected()) {
            logger.info("Not connected to a server");
            return;
        }

        application.disconnect();
    }
}
