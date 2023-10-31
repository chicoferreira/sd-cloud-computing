package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.logging.impl.StdoutLogger;
import sd.cloudcomputing.common.logging.impl.ThreadPrefixedLoggerFormat;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Worker {

    private final Logger logger;

    private final WorkerScheduler workerScheduler;
    private final BoundedBuffer<JobResult> queuedJobResults;

    private boolean running;
    private ServerSocket serverSocket;

    private Thread jobResultHandlerThread;

    public Worker(int maxMemoryCapacity, int maxConcurrentJobs) {
        this.logger = new StdoutLogger(new ThreadPrefixedLoggerFormat());

        JobExecutor jobExecutor = new JobExecutor();
        this.workerScheduler = new WorkerScheduler(this.logger, jobExecutor, maxMemoryCapacity, maxConcurrentJobs, this::receiveJobResult);

        this.queuedJobResults = new BoundedBuffer<>(100);
    }

    public void run(int port) {
        setupShutdownHook();

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

            // get the input stream from the connected socket
            while (running) {
                try {
                    InputStream inputStream = socket.getInputStream();

                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                    Object object = objectInputStream.readObject();
                    if (object instanceof JobRequest jobRequest) {
                        this.workerScheduler.queue(jobRequest);
                    } else {
                        this.logger.warn("Received unknown object: " + object);
                    }
                } catch (EOFException e) {
                    this.running = false;
                    logger.info("Server disconnected");
                } catch (ClassNotFoundException | IOException e) {
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
        }));
    }
}
