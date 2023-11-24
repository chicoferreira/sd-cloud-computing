package sd.cloudcomputing.client.job;

import sd.cloudcomputing.common.JobResult;

/**
 * Used to store client information about a job
 */
public sealed interface ClientJob permits ClientJob.Scheduled, ClientJob.Finished {
    int jobId();

    String jobOutputFile();

    int memory();

    long timestampCreated();

    record Scheduled(int jobId, String jobOutputFile, int memory, long timestampCreated) implements ClientJob {
        public Finished toFinished(JobResult jobResult) {
            return new Finished(jobId, jobOutputFile, memory, timestampCreated, jobResult);
        }
    }

    record Finished(int jobId, String jobOutputFile, int memory, long timestampCreated,
                    JobResult jobResult) implements ClientJob {
    }
}