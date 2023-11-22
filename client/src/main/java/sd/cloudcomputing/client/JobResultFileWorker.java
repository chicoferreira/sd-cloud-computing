package sd.cloudcomputing.client;

import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class JobResultFileWorker {

    private static final Path resultFolder = Path.of("results");

    private final Logger logger;
    private final BoundedBuffer<JobResult.Success> queue;
    private Thread thread;
    private boolean running;

    public JobResultFileWorker(Logger logger) {
        this.logger = logger;
        this.queue = new BoundedBuffer<>(100);
    }

    public void queueWrite(JobResult.Success success) throws InterruptedException {
        this.queue.put(success);
    }

    public void run() {
        this.running = true;
        this.thread = new Thread(() -> {
            while (this.running) {
                try {
                    JobResult.Success success = queue.take();
                    save(success);
                } catch (InterruptedException ignored) {
                }
            }
        }, "Job-Result-File-Writer-Worker-Thread");
        this.thread.start();
    }

    private void save(JobResult.Success success) {
        String fileName = getFileName(success);
        Path path = resultFolder.resolve(fileName);
        try {
            this.logger.info("Saving job result for job " + success.jobId() + " to file '" + fileName + "'...");
            Files.createDirectories(resultFolder);
            Files.write(path, success.data(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            this.logger.error("Could not save job result for job " + success.jobId() + " to file '" + fileName + "': " + e.getMessage());
        }
    }

    public String getFileName(JobResult.Success success) {
        return "job-" + success.jobId() + ".7z"; // The JobFunction will create bytes of a .7z file on success
    }

    public void stop() {
        this.running = false;
        if (this.thread != null) {
            this.thread.interrupt();
        }
    }

}
