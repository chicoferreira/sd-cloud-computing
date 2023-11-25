package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;

public interface JobScheduler {
    boolean scheduleJob(JobRequest jobRequest);
}
