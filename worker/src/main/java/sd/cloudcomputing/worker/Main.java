package sd.cloudcomputing.worker;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption("h", "help", false, "Prints this help message");
        options.addOption("p", "port", true, "Port to listen on");
        options.addOption("m", "memory-capacity", true, "Maximum memory capacity");
        options.addOption("j", "max-concurrent-jobs", true, "Maximum concurrent jobs");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(args[0], options);
            return;
        }

        int maxMemoryCapacity = 1000;
        int maxConcurrentJobs = 10;

        if (commandLine.getParsedOptionValue("memory-capacity") instanceof Number number) {
            maxMemoryCapacity = number.intValue();
        }

        if (commandLine.getParsedOptionValue("max-concurrent-jobs") instanceof Number number) {
            maxConcurrentJobs = number.intValue();
        }

        Worker worker = new Worker(maxMemoryCapacity, maxConcurrentJobs);
        worker.run(9900);
    }
}
