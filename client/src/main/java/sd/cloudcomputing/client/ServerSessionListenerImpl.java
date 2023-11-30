package sd.cloudcomputing.client;

import sd.cloudcomputing.client.api.ClientJob;
import sd.cloudcomputing.client.api.JobResultFileWorker;
import sd.cloudcomputing.client.api.ServerSession;
import sd.cloudcomputing.client.api.ServerSessionSaveToFileListener;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.SCServerStatusResponsePacket;

public class ServerSessionListenerImpl extends ServerSessionSaveToFileListener {

    private final Application application;
    private final Logger logger;

    public ServerSessionListenerImpl(Application application, Logger logger, JobResultFileWorker jobResultFileWorker) {
        super(jobResultFileWorker);
        this.application = application;
        this.logger = logger;
    }

    @Override
    public void onServerStatusResponse(ServerSession serverSession, SCServerStatusResponsePacket serverStatusResponse) {
        this.logger.info("Server status (" + serverSession.getAddressWithPort() + "): ");
        this.logger.info(" - Connected workers: " + serverStatusResponse.connectedWorkers());
        this.logger.info(" - Total capacity: " + serverStatusResponse.totalCapacity() + "MB");
        this.logger.info(" - Max possible memory: " + serverStatusResponse.maxPossibleMemory() + "MB");
        this.logger.info(" - Memory usage: " + serverStatusResponse.memoryUsagePercentage() + "%");
        this.logger.info(" - Jobs running: " + serverStatusResponse.jobsCurrentlyRunning());
    }

    @Override
    public void onJobResultResponse(ServerSession serverSession, ClientJob.Received clientJob) {
        switch (clientJob.jobResult()) {
            case JobResult.Success success ->
                    logger.info("Job " + success.jobId() + " completed successfully with " + success.data().length + " bytes.");
            case JobResult.Failure failure ->
                    logger.error("Job " + failure.jobId() + " failed with error code " + failure.errorCode() + ": " + failure.errorMessage());
            case JobResult.NoMemory noMemory ->
                    logger.info("Job " + noMemory.jobId() + " failed due to not enough memory");
        }

        super.onJobResultResponse(serverSession, clientJob);
    }

    @Override
    public void onJobResultWriteToFile(JobResult.Success success, String outputFile) {
        logger.info("Saved job result for job " + success.jobId() + " to file " + outputFile);
    }

    @Override
    public void onDisconnect(ServerSession serverSession) {
        application.notifyServerDisconnect();
    }
}
