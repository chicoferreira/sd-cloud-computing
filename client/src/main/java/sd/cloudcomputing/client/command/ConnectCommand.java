package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.common.logging.Logger;

import java.util.OptionalInt;

public class ConnectCommand extends AbstractCommand {

    private final Application application;

    public ConnectCommand(Application application) {
        super("connect", "<host> <port>");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        if (application.isConnected()) {
            logger.info("Already connected. Use disconnect to disconnect.");
            return;
        }

        if (args.length < 2) {
            sendUsage(logger);
            return;
        }

        String host = args[0];
        OptionalInt port = parseInt(args[1]);
        if (port.isEmpty()) {
            sendUsage(logger);
            return;
        }

        application.connect(host, port.getAsInt());
    }
}
