package sd.cloudcomputing.client.job;

import sd.cloudcomputing.common.JobResult;

/**
 * Used to store client information about a job
 */
public sealed interface ClientJob permits ClientJob.Received, ClientJob.Scheduled {
    int jobId();

    String jobOutputFile();

    int memory();

    long timestampCreated();

    record Scheduled(int jobId, String jobOutputFile, int memory, long timestampCreated) implements ClientJob {
        public Received toFinished(JobResult jobResult) {
            return new Received(jobId, jobOutputFile, memory, timestampCreated, jobResult);
        }
    }

    record Received(int jobId, String jobOutputFile, int memory, long timestampCreated,
                    JobResult jobResult) implements ClientJob {
    }
}