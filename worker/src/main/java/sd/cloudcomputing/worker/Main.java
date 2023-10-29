package sd.cloudcomputing.worker;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption("m", "memory-capacity", true, "Maximum memory capacity");
        options.addOption("j", "max-concurrent-jobs", true, "Maximum concurrent jobs");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        int maxMemoryCapacity = 1000;
        int maxConcurrentJobs = 10;

        if (commandLine.getParsedOptionValue("memory-capacity") instanceof Number number) {
            maxMemoryCapacity = number.intValue();
        }

        if (commandLine.getParsedOptionValue("max-concurrent-jobs") instanceof Number number) {
            maxConcurrentJobs = number.intValue();
        }

        Worker worker = new Worker(maxMemoryCapacity, maxConcurrentJobs);
        worker.start(9900);
    }
}
