package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;

import java.util.ArrayList;
import java.util.List;

public class Client {

    private final String name;
    private final String password;
    private final List<JobRequest> requestedJobs;
    private final List<JobResult> completedJobs;

    public Client(String name, String password) {
        this.name = name;
        this.password = password;
        this.requestedJobs = new ArrayList<>();
        this.completedJobs = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return this.password;
    }

    public void registerJob(JobRequest jobRequest) {
        this.requestedJobs.add(jobRequest);
    }

    public void completeJob(JobResult jobResult) {
        this.completedJobs.add(jobResult);
    }

}
