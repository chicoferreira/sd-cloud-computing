package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Client;
import sd.cloudcomputing.common.logging.Logger;

import java.util.OptionalInt;

public class ConnectCommand extends AbstractCommand {

    private final Client client;

    public ConnectCommand(Client client) {
        super("connect", "<host> <port>");
        this.client = client;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        if (client.isConnected()) {
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

        client.connect(host, port.getAsInt());
    }
}
