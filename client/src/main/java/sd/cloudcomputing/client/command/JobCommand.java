package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.client.ServerConnection;
import sd.cloudcomputing.common.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JobCommand extends AbstractCommand {

    private final Application application;

    public JobCommand(Application application) {
        super("job", "<file> <memory>");
        this.application = application;
    }

    private static byte[] getFileBytes(String[] args) throws IOException {
        Path path = Path.of(args[0]);

        return Files.readAllBytes(path);
    }

    @Override
    public void execute(Logger logger, String[] args) {
        if (args.length < 2) {
            sendUsage(logger);
            return;
        }

        byte[] bytes;
        try {
            bytes = getFileBytes(args);
        } catch (IOException e) {
            logger.error("Failed to read file: " + e.getMessage());
            return;
        }

        int memory = parseInt(args[1]).orElse(-1);
        if (memory < 0) {
            logger.error("Memory must be a positive integer");
            sendUsage(logger);
            return;
        }

        boolean connected = application.isConnected();
        if (!connected) {
            logger.error("Not connected to server");
            return;
        }

        ServerConnection currentServerConnection = application.getCurrentServerConnection();

        int jobId = currentServerConnection.scheduleJob(bytes, memory);
        logger.info("Scheduled job with id " + jobId);
    }

}
