package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.client.api.ClientJob;
import sd.cloudcomputing.common.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JobsCommand extends AbstractCommand {

    private final Application application;

    private static final int MAX_JOBS_SHOWN = 7;

    public JobsCommand(Application application) {
        super("jobs", "");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        List<ClientJob> allJobs = application.getAllJobs();

        List<ClientJob.Scheduled> scheduledJobs = new ArrayList<>();
        List<ClientJob.Received> receivedJobs = new ArrayList<>();

        for (ClientJob job : allJobs) {
            switch (job) {
                case ClientJob.Received received -> receivedJobs.add(received);
                case ClientJob.Scheduled scheduled -> scheduledJobs.add(scheduled);
            }
        }

        logger.info("");
        logger.info("JOBS (" + scheduledJobs.size() + " scheduled, " + receivedJobs.size() + " finished, " + allJobs.size() + " total)");
        logger.info("");
        logger.info(" Scheduled jobs (" + scheduledJobs.size() + "):");
        scheduledJobs.stream()
                .sorted(Comparator.comparing(ClientJob.Scheduled::nanoTimeCreated).reversed())
                .limit(MAX_JOBS_SHOWN)
                .forEach(job -> logger.info(String.format(" %4s  %10s  %5sMB  %7ss ago",
                        job.jobId(),
                        job.jobOutputFile(),
                        job.memory(),
                        secondsAgo(job.nanoTimeCreated()))));
        if (scheduledJobs.size() > MAX_JOBS_SHOWN) {
            int missing = scheduledJobs.size() - MAX_JOBS_SHOWN;
            logger.info(String.format(" %10s more...", missing));
        }

        logger.info("");
        logger.info(" Received jobs (" + receivedJobs.size() + "):");
        receivedJobs.stream()
                .sorted(Comparator.comparing(ClientJob.Received::nanoTimeReceived).reversed())
                .limit(MAX_JOBS_SHOWN)
                .forEach(job -> logger.info(String.format(" %4s  %10s  %5sMB  %7ss ago  %s",
                        job.jobId(),
                        job.jobOutputFile(),
                        job.memory(),
                        secondsAgo(job.nanoTimeReceived()),
                        job.jobResult().resultType().name())));
        if (receivedJobs.size() > MAX_JOBS_SHOWN) {
            int missing = receivedJobs.size() - MAX_JOBS_SHOWN;
            logger.info(String.format(" %s more...", missing));
        }
        logger.info("");
    }

    private long secondsAgo(long nanoTimeSince) {
        return (System.nanoTime() - nanoTimeSince) / 1_000_000_000;
    }
}
