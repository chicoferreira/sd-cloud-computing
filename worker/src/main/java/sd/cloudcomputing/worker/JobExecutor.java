package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd23.JobFunction;
import sd23.JobFunctionException;

public class JobExecutor {

    public JobResult execute(JobRequest jobRequest) {
        try {
            byte[] execute = JobFunction.execute(jobRequest.getData());
            return JobResult.success(jobRequest.getJobId(), execute);
        } catch (JobFunctionException e) {
            return JobResult.failure(jobRequest.getJobId(), e.getCode(), e.getMessage());
        }
    }
}
