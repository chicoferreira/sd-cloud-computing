package sd.cloudcomputing.client.job;

import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Class responsible for writing job results to files in a queued manner.
 */
public class JobResultFileWorker {

    private static final Path resultFolder = Path.of("results");

    private final BoundedBuffer<JobResultFile> queue;

    private final Logger logger;

    public void queueWrite(JobResult.Success success, String fileName) throws InterruptedException {
        this.queue.put(new JobResultFile(fileName, success));
    }
    private Thread thread;
    private boolean running;

    public JobResultFileWorker(Logger logger) {
        this.logger = logger;
        this.queue = new BoundedBuffer<>(100);
    }

    public void run() {
        this.running = true;
        this.thread = new Thread(() -> {
            while (this.running) {
                try {
                    JobResultFile success = queue.take();
                    save(success.jobResult(), success.fileName());
                } catch (InterruptedException ignored) {
                }
            }
        }, "Job-Result-File-Writer-Worker-Thread");
        this.thread.start();
    }

    private void save(JobResult.Success success, String fileName) {
        Path path = resultFolder.resolve(fileName);
        try {
            this.logger.info("Saving job result for job " + success.jobId() + " to file '" + fileName + "'...");
            Files.createDirectories(resultFolder);
            Files.write(path, success.data(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            this.logger.error("Could not save job result for job " + success.jobId() + " to file '" + fileName + "': " + e.getMessage());
        }
    }

    private record JobResultFile(String fileName, JobResult.Success jobResult) {
    }

    public void stop() {
        this.running = false;
        if (this.thread != null) {
            this.thread.interrupt();
        }
    }

}
