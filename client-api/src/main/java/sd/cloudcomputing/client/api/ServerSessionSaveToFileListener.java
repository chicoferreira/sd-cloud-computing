package sd.cloudcomputing.client.api;

import sd.cloudcomputing.common.JobResult;

public abstract class ServerSessionSaveToFileListener implements ServerSessionListener {

    private final JobResultFileWorker jobResultFileWorker;

    protected ServerSessionSaveToFileListener(JobResultFileWorker jobResultFileWorker) {
        this.jobResultFileWorker = jobResultFileWorker;
    }

    @Override
    public void onJobResultResponse(ServerSession serverSession, ClientJob.Received clientJob) {
        JobResult jobResult = clientJob.jobResult();
        if (jobResult instanceof JobResult.Success success) {
            try {
                jobResultFileWorker.queueWrite(success, clientJob.jobOutputFile());
                onJobResultWriteToFile(success, clientJob.jobOutputFile());
            } catch (InterruptedException ignored) {
            }
        }
    }

    public abstract void onJobResultWriteToFile(JobResult.Success success, String outputFile);

}
