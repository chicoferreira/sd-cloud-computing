package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;

public interface JobScheduler {
    /**
     * @param jobRequest The job request to schedule
     * @return True when the job request was scheduled (or queued) successfully, false when there is no enough memory capacity available for the job
     */
    boolean scheduleJob(JobRequest jobRequest);
}
