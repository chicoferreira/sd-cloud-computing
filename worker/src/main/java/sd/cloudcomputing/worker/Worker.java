package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.AbstractLogger;
import sd.cloudcomputing.common.logging.impl.StdoutLogger;
import sd.cloudcomputing.common.logging.impl.ThreadPrefixedLoggerFormat;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.*;
import java.net.Socket;

public class Worker {

    private final AbstractLogger logger;

    private final WorkerScheduler workerScheduler;
    private final BoundedBuffer<JobResult> queuedJobResults;
    private final Frost frost;

    private boolean running;

    public Worker(Frost frost, int maxMemoryCapacity, int maxConcurrentJobs) {
        this.frost = frost;
        this.logger = new StdoutLogger(new ThreadPrefixedLoggerFormat());
        this.logger.hookSystemPrint();

        JobExecutor jobExecutor = new JobExecutor();
        this.workerScheduler = new WorkerScheduler(this.logger, jobExecutor, maxMemoryCapacity, maxConcurrentJobs, this::receiveJobResult);

        this.queuedJobResults = new BoundedBuffer<>(100);
    }

    public void run(String host, int port) {
        setupShutdownHook();

        logger.info("Starting worker with max memory capacity of " + workerScheduler.getMaxMemoryCapacity() + " and max concurrent jobs of " + workerScheduler.getMaxConcurrentJobs());

        Thread serverThread = new Thread(() -> runServer(host, port), "Worker-Server-Thread");
        serverThread.start();

        try {
            serverThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.running = false;
    }

    private void runServer(String host, int port) {
        running = true;

        logger.info("Connecting to server on " + host + ":" + port);
        try (Socket socket = new Socket(host, port)) {
            logger.info("Server connected. Waiting for requests...");

            Thread jobResultHandlerThread = new Thread(() -> handleJobResults(socket), "Worker-Job-Result-Handler-Thread");
            jobResultHandlerThread.start();

            this.workerScheduler.start();

            while (running) {
                try {
                    InputStream inputStream = socket.getInputStream();

                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));

                    SerializeInput serializeInput = new SerializeInput(dataInputStream);
                    JobRequest jobRequest = frost.readSerializable(JobRequest.class, serializeInput);

                    logger.info("Received job request with id " + jobRequest.jobId() + " and " + jobRequest.data().length + " bytes of data");
                    workerScheduler.queue(jobRequest);
                } catch (IOException e) {
                    this.running = false;
                    logger.info("Server disconnected");
                } catch (SerializationException e) {
                    logger.error("Couldn't parse object: ", e);
                }
            }

            this.workerScheduler.stop();
            jobResultHandlerThread.interrupt();
        } catch (IOException e) {
            logger.error("Failed to create server: ", e);
        }
    }

    private void receiveJobResult(JobResult jobResult) {
        try {
            queuedJobResults.put(jobResult);
        } catch (InterruptedException ignored) {
        }
    }

    private void handleJobResults(Socket socket) {
        try {
            while (running) {
                JobResult jobResult = queuedJobResults.take();
                if (jobResult.getResultType() == JobResult.ResultType.FAILURE) {
                    logger.info("Job failed with error code " + jobResult.getErrorCode() + ": " + jobResult.getErrorMessage());
                } else {
                    logger.info("Job succeeded with result: " + jobResult.getData().length + " bytes");
                }

                try {
                    SerializeOutput output = new SerializeOutput(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
                    frost.writeSerializable(jobResult, JobResult.class, output);
                    frost.flush(output);
                } catch (IOException | SerializationException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down worker...");
            running = false;
        }, "Worker-Shutdown-Hook"));
    }
}
