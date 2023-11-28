package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.logging.AbstractLogger;
import sd.cloudcomputing.common.logging.impl.StdoutLogger;
import sd.cloudcomputing.common.logging.impl.ThreadPrefixedLoggerFormat;
import sd.cloudcomputing.common.serialization.Frost;

import java.io.IOException;
import java.net.Socket;

public class Worker {

    private final AbstractLogger logger;

    private final WorkerScheduler workerScheduler;
    private final Frost frost;
    private ServerConnection serverConnection;


    public Worker(Frost frost, int maxMemoryCapacity, int maxConcurrentJobs) {
        this.frost = frost;
        this.logger = new StdoutLogger(new ThreadPrefixedLoggerFormat());
        this.logger.hookSystemPrint();

        this.workerScheduler = new WorkerScheduler(this.logger, new JobExecutor(), maxMemoryCapacity, maxConcurrentJobs, this::handlePacket);
    }

    public void run(String host, int port) {
        setupShutdownHook();

        logger.info("Starting worker with max memory capacity of " + workerScheduler.getMaxMemoryCapacity() + " and max concurrent jobs of " + workerScheduler.getMaxConcurrentJobs());

        runServer(host, port);
    }

    public void stop() {
        if (this.serverConnection != null) {
            this.serverConnection.disconnect();
        }
        if (this.workerScheduler != null) {
            this.workerScheduler.stop();
        }

        logger.info("Shutting down worker...");
    }

    private void runServer(String host, int port) {
        logger.info("Connecting to server on " + host + ":" + port);
        try (ServerConnection serverConnection = new ServerConnection(this.logger, this.frost, new Socket(host, port), this.workerScheduler, this)) {
            logger.info("Server connected. Sending handshake and waiting for requests...");
            this.workerScheduler.start();

            serverConnection.start();
            this.serverConnection = serverConnection;

            serverConnection.join();

            stop();
        } catch (IOException e) {
            logger.error("Failed to create server: " + e.getMessage());
        }
    }

    private void handlePacket(JobResult packet) {
        this.serverConnection.enqueuePacket(packet);
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Worker-Shutdown-Hook"));
    }
}
