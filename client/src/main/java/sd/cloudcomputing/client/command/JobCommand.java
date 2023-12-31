package sd.cloudcomputing.client.command;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.common.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JobCommand extends AbstractCommand {

    private final Application application;

    public JobCommand(Application application) {
        super("job", "<file> <memory> (output path)");
        this.application = application;
    }

    private static byte @Nullable [] getFileBytes(String pathString) throws IOException {
        Path path = Path.of(pathString);

        return Files.readAllBytes(path);
    }

    @Override
    public void execute(Logger logger, String[] args) {
        if (args.length < 2) {
            sendUsage(logger);
            return;
        }

        byte[] bytes;
        String filePath = args[0];
        try {
            bytes = getFileBytes(filePath);
        } catch (IOException e) {
            logger.error("Failed to read file: " + e.getMessage());
            return;
        }

        if (bytes == null) {
            logger.error("Failed to read file " + filePath);
            return;
        }

        int memory = parseInt(args[1]).orElse(-1);
        if (memory < 0) {
            logger.error("Memory must be a positive integer");
            sendUsage(logger);
            return;
        }

        if (!application.isConnected()) {
            logger.error("Not connected to server");
            return;
        }

        String outputFile = args.length > 2 ? args[2] : null;

        int jobId = application.createAndScheduleJobRequest(bytes, memory, outputFile);
        if (outputFile != null) {
            logger.info("Sent job with id " + jobId + " with " + bytes.length + " bytes of data (output file: " + outputFile + ")");
        } else {
            logger.info("Sent job with id " + jobId + " with " + bytes.length + " bytes of data");
        }
    }

}
