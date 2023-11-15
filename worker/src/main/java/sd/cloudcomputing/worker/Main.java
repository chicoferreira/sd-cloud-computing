package sd.cloudcomputing.worker;

import org.apache.commons.cli.*;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.protocol.WSHandshakePacket;
import sd.cloudcomputing.common.serialization.Frost;

public class Main {
    public static void main(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption("h", "help", false, "Prints this help message");
        options.addOption("c", "connect", true, "The server to connect on");
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

        String host = "localhost";
        int port = 9900;

        if (commandLine.getParsedOptionValue("connect") instanceof String address) {
            String[] split = address.split(":");
            if (split.length != 2) {
                throw new RuntimeException("Invalid address");
            }

            host = split[0];
            port = Integer.parseInt(split[1]);
        }

        Frost frost = new Frost();
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());
        frost.registerSerializer(WSHandshakePacket.class, new WSHandshakePacket.Serialization());

        Worker worker = new Worker(frost, maxMemoryCapacity, maxConcurrentJobs);
        worker.run(host, port);
    }
}
