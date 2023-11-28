package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.WorkerJobResult;
import sd23.JobFunction;
import sd23.JobFunctionException;

public class JobExecutor {

    public WorkerJobResult execute(JobRequest jobRequest) {
        try {
            byte[] execute = JobFunction.execute(jobRequest.data());
            return new WorkerJobResult.Success(jobRequest.jobId(), execute);
        } catch (JobFunctionException e) {
            return new WorkerJobResult.Failure(jobRequest.jobId(), e.getCode(), e.getMessage());
        }
    }
}
