package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd23.JobFunction;
import sd23.JobFunctionException;

import java.util.function.Supplier;

public class JobExecutor implements Supplier<JobResult> {

    private final JobRequest jobRequest;

    public JobExecutor(JobRequest jobRequest) {
        this.jobRequest = jobRequest;
    }

    @Override
    public JobResult get() {
        try {
            byte[] execute = JobFunction.execute(this.jobRequest.getData());
            return JobResult.success(execute);
        } catch (JobFunctionException e) {
            return JobResult.failure(e.getCode(), e.getMessage());
        }
    }
}
