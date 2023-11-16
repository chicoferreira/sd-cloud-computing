package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd23.JobFunction;
import sd23.JobFunctionException;

public class JobExecutor {

    public JobResult execute(JobRequest jobRequest) {
        try {
            byte[] execute = JobFunction.execute(jobRequest.data());
            return JobResult.success(jobRequest.jobId(), execute);
        } catch (JobFunctionException e) {
            return JobResult.failure(jobRequest.jobId(), e.getCode(), e.getMessage());
        }
    }
}
