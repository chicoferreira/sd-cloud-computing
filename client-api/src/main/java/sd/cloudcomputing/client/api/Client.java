package sd.cloudcomputing.client.api;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedInteger;
import sd.cloudcomputing.common.protocol.*;
import sd.cloudcomputing.common.serialization.Frost;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Client {

    private final JobManager jobManager;
    private final Frost frost;
    private final SynchronizedInteger jobIdCounter = new SynchronizedInteger(0);

    private Client(Frost frost) {
        this.frost = frost;
        this.jobManager = new JobManager();
    }

    public static Client createNewClient() {
        Frost frost = new Frost();
        frost.registerSerializer(CSAuthPacket.class, new CSAuthPacket.Serialization());
        frost.registerSerializer(SCAuthResult.class, new SCAuthResult.Serialization());
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());
        frost.registerSerializer(CSServerStatusRequestPacket.class, new CSServerStatusRequestPacket.Serialization());
        frost.registerSerializer(SCServerStatusResponsePacket.class, new SCServerStatusResponsePacket.Serialization());

        GenericPacketSerializer genericSerializer = new GenericPacketSerializer();
        genericSerializer.registerPacketId(JobRequest.PACKET_ID, JobRequest.class);
        genericSerializer.registerPacketId(JobResult.PACKET_ID, JobResult.class);
        genericSerializer.registerPacketId(SCServerStatusResponsePacket.PACKET_ID, SCServerStatusResponsePacket.class);
        genericSerializer.registerPacketId(CSServerStatusRequestPacket.PACKET_ID, CSServerStatusRequestPacket.class);
        frost.registerSerializer(GenericPacket.class, genericSerializer);

        return new Client(frost);
    }

    public ServerNoAuthSession connect(String ip, int port) throws IOException {
        Socket socket = new Socket(ip, port);
        return new ServerNoAuthSession(socket, this.frost);
    }

    public int createAndScheduleJobRequest(ServerSession serverSession, byte[] bytes, int neededMemory, @Nullable String outputFileName) {
        int jobId = this.jobIdCounter.getAndIncrement();
        serverSession.scheduleJob(jobId, bytes, neededMemory);

        if (outputFileName == null) {
            outputFileName = getDefaultFileName(jobId);
        }

        ClientJob clientJob = new ClientJob.Scheduled(jobId, outputFileName, neededMemory, System.nanoTime());
        this.jobManager.addJob(clientJob);
        return jobId;
    }

    private String getDefaultFileName(int jobId) {
        return "job-" + jobId + ".7z"; // The JobFunction will create bytes of a .7z file on success
    }

    public List<ClientJob> getAllJobs() {
        return this.jobManager.getAllJobs();
    }

    public JobManager getJobManager() {
        return jobManager;
    }
}
