package sd.cloudcomputing.client.api;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;

import java.util.List;

public class ClientJobManager {

    private final SynchronizedMap<Integer, ClientJob> jobs = new SynchronizedMap<>();

    public void addJob(ClientJob clientJob) {
        jobs.put(clientJob.jobId(), clientJob);
    }

    public @Nullable ClientJob.Received registerJobResult(int jobId, JobResult jobResult) {
        ClientJob clientJob = jobs.remove(jobId);

        if (clientJob instanceof ClientJob.Scheduled scheduled) { // this already null checks
            ClientJob.Received received = scheduled.toFinished(jobResult, System.nanoTime());
            jobs.put(jobId, received);
            return received;
        }
        return null;
    }

    public List<ClientJob> getAllJobs() {
        return jobs.values();
    }

}
