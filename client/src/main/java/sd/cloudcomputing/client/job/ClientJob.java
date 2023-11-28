package sd.cloudcomputing.client.job;

import sd.cloudcomputing.common.JobResult;

/**
 * Used to store client information about a job
 */
public sealed interface ClientJob permits ClientJob.Received, ClientJob.Scheduled {
    int jobId();

    String jobOutputFile();

    int memory();

    long nanoTimeCreated();

    record Scheduled(int jobId, String jobOutputFile, int memory, long nanoTimeCreated) implements ClientJob {
        public Received toFinished(JobResult jobResult, long nanoTimeReceived) {
            return new Received(jobId, jobOutputFile, memory, nanoTimeCreated, nanoTimeReceived, jobResult);
        }
    }

    record Received(int jobId, String jobOutputFile, int memory, long nanoTimeCreated, long nanoTimeReceived,
                    JobResult jobResult) implements ClientJob {
    }
}