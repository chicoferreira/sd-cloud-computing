package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.protocol.WSHandshakePacket;
import sd.cloudcomputing.common.serialization.Frost;

public class Main {

    // worker localhost:8080 100 1000
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar worker.jar <host>:<port> <maxConcurrentJobs> <maxMemoryCapacity>");
            return;
        }

        String[] hostPort = args[0].split(":");
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        int maxConcurrentJobs = Integer.parseInt(args[1]);
        int maxMemoryCapacity = Integer.parseInt(args[2]);

        Frost frost = new Frost();
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());
        frost.registerSerializer(WSHandshakePacket.class, new WSHandshakePacket.Serialization());

        Worker worker = new Worker(frost, maxMemoryCapacity, maxConcurrentJobs);
        worker.run(host, port);
    }
}
