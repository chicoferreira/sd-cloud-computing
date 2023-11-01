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

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Worker {

    private final AbstractLogger logger;

    private final WorkerScheduler workerScheduler;
    private final BoundedBuffer<JobResult> queuedJobResults;
    private final Frost frost;

    private boolean running;
    private ServerSocket serverSocket;

    private Thread jobResultHandlerThread;

    public Worker(Frost frost, int maxMemoryCapacity, int maxConcurrentJobs) {
        this.frost = frost;
        this.logger = new StdoutLogger(new ThreadPrefixedLoggerFormat());
        this.logger.hookSystemPrint();

        JobExecutor jobExecutor = new JobExecutor();
        this.workerScheduler = new WorkerScheduler(this.logger, jobExecutor, maxMemoryCapacity, maxConcurrentJobs, this::receiveJobResult);

        this.queuedJobResults = new BoundedBuffer<>(100);
    }

    public void run(int port) {
        setupShutdownHook();

        logger.info("Starting worker with max memory capacity of " + workerScheduler.getMaxMemoryCapacity() + " and max concurrent jobs of " + workerScheduler.getMaxConcurrentJobs());

        Thread serverThread = new Thread(() -> runServer(port), "Worker-Server-Thread");
        serverThread.start();

        this.jobResultHandlerThread = new Thread(this::handleJobResults, "Worker-Job-Result-Handler-Thread");
        jobResultHandlerThread.start();

        this.workerScheduler.start();

        try {
            serverThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.running = false;

        stop();
    }

    private void initServer(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
            logger.info("Worker listening on port " + port);
        } catch (IOException ex) {
            logger.error("Couldn't start server in port " + port, ex);
        }
    }

    private void runServer(int port) {
        running = true;
        this.initServer(port);

        try {
            Socket socket = serverSocket.accept();
            logger.info("Server connected. Waiting for requests...");

            while (running) {
                try {
                    InputStream inputStream = socket.getInputStream();

                    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));

                    SerializeInput serializeInput = new SerializeInput(dataInputStream);
                    JobRequest jobRequest = frost.readSerializable(JobRequest.class, serializeInput);
                    if (jobRequest == null) {
                        logger.warn("Received unkwown bytes.");
                        continue;
                    }
                    logger.info("Received job request with id " + jobRequest.getJobId() + " and " + jobRequest.getData().length + " bytes of data");
                    workerScheduler.queue(jobRequest);
                } catch (SerializationException e) {
                    if (e.getCause() instanceof EOFException) {
                        this.running = false;
                        logger.info("Server disconnected");
                        return;
                    }
                    if (e.getCause() instanceof SocketException) {
                        this.running = false;
                        logger.error("Socket error", e.getCause());
                        return;
                    }

                    logger.error("Couldn't parse object: ", e);
                }
            }
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

    private void handleJobResults() {
        try {
            while (running) {
                JobResult jobResult = queuedJobResults.take();
                if (jobResult.getResultType() == JobResult.ResultType.FAILURE) {
                    logger.info("Job failed with error code " + jobResult.getErrorCode() + ": " + jobResult.getErrorMessage());
                } else {
                    logger.info("Job succeeded with result: " + jobResult.getData().length + " bytes");
                }

                // TODO: send result back to server
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void stop() {
        try {
            this.jobResultHandlerThread.interrupt();
            this.workerScheduler.stop();
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Failed to close server socket", e);
        }
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down worker...");
            running = false;
        }, "Worker-Shutdown-Hook"));
    }
}
