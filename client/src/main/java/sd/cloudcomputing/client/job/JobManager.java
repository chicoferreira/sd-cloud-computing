package sd.cloudcomputing.client.job;

import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;

import java.util.List;

public class JobManager {

    private final SynchronizedMap<Integer, ClientJob> jobs = new SynchronizedMap<>();

    public void addJob(ClientJob clientJob) {
        jobs.put(clientJob.jobId(), clientJob);
    }

    /**
     * @param jobId The job ID
     * @return The job associated with the given ID or null if no job with the given ID
     * <p>
     * The returned job (if exists) can be either scheduled or finished
     */
    public ClientJob getJob(int jobId) {
        return jobs.get(jobId);
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
