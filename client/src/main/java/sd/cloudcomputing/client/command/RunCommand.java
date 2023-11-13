package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.ClientPacketDispatcher;
import sd.cloudcomputing.common.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RunCommand extends AbstractCommand {

    private final ClientPacketDispatcher packetDispatcher;

    public RunCommand(ClientPacketDispatcher packetDispatcher) {
        super("run", "<file> <memory>");
        this.packetDispatcher = packetDispatcher;
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

        try {
            int jobId = packetDispatcher.scheduleJob(bytes, memory);
            logger.info("Scheduled job with id " + jobId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
