package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.logging.AbstractLogger;
import sd.cloudcomputing.common.logging.impl.StdoutLogger;
import sd.cloudcomputing.common.logging.impl.ThreadPrefixedLoggerFormat;
import sd.cloudcomputing.common.protocol.WSHandshakePacket;
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
    private final int maxMemoryCapacity;

    private boolean running;

    public Worker(Frost frost, int maxMemoryCapacity, int maxConcurrentJobs) {
        this.frost = frost;
        this.maxMemoryCapacity = maxMemoryCapacity;
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
            logger.info("Server connected. Sending handshake and waiting for requests...");

            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            SerializeOutput output = new SerializeOutput(dataOutputStream);

            frost.writeSerializable(new WSHandshakePacket(this.maxMemoryCapacity), WSHandshakePacket.class, output);
            frost.flush(output);

            Thread jobResultHandlerThread = new Thread(() -> handleJobResults(socket), "Worker-Job-Result-Handler-Thread");
            jobResultHandlerThread.start();

            this.workerScheduler.start();

            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            SerializeInput serializeInput = new SerializeInput(dataInputStream);
            while (running) {
                try {
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
        } catch (SerializationException e) {
            logger.error("Failed to serialize handshake packet: ", e);
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
                    logger.info("Job " + jobResult.getJobId() + "failed with error code " + jobResult.getErrorCode() + ": " + jobResult.getErrorMessage());
                } else {
                    logger.info("Job " + jobResult.getJobId() + " succeeded with result: " + jobResult.getData().length + " bytes");
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
