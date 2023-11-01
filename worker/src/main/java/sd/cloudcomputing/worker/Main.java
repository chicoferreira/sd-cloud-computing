package sd.cloudcomputing.worker;

import org.apache.commons.cli.*;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.serialization.Frost;

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

        int port = 9900;

        if (commandLine.getParsedOptionValue("port") instanceof Number number) {
            port = number.intValue();
        }

        Frost frost = new Frost();
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());

        Worker worker = new Worker(frost, maxMemoryCapacity, maxConcurrentJobs);
        worker.run(port);
    }
}
