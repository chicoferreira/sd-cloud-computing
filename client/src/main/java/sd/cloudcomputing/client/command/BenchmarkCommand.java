package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.common.logging.Logger;

import java.util.OptionalInt;

public class BenchmarkCommand extends AbstractCommand {
    private final Application application;

    public BenchmarkCommand(Application application) {
        super("benchmark", "<numberXmemory>...");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        if (!application.isConnected()) {
            logger.error("Not connected to server");
            return;
        }

        for (String arg : args) {
            String[] parts = arg.split("x");
            OptionalInt number = parseInt(parts[0]);
            OptionalInt memory = parseInt(parts[1]);

            if (number.isPresent() && memory.isPresent()) {
                logger.info("Sending " + number.getAsInt() + " jobs with " + memory.getAsInt() + "MB memory");
                for (int i = 0; i < number.getAsInt(); i++) {
                    application.createAndScheduleJobRequest(new byte[]{}, memory.getAsInt(), null);
                }
            } else {
                logger.error("Invalid argument: " + arg + " (expected: <number>x<memory>)");
            }
        }
    }
}
