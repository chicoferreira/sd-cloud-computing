package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.WSHandshakePacket;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class WorkerConnection {

    private final Logger logger;
    private final Socket socket;
    private final Frost frost;
    private final Consumer<JobResult> handleJobResult;
    private final Consumer<WorkerConnection> onDisconnect;
    private int maxMemoryCapacity;
    private boolean running;

    private final SynchronizedMap<Integer, JobRequest> pendingJobRequests;

    private final BoundedBuffer<JobRequest> queuedJobRequests;
    private Thread readThread;
    private Thread writeThread;

    public WorkerConnection(Logger logger, Frost frost, Socket socket, Consumer<JobResult> handleJobResult, Consumer<WorkerConnection> onDisconnect) {
        this.logger = logger;
        this.frost = frost;
        this.socket = socket;
        this.handleJobResult = handleJobResult;
        this.queuedJobRequests = new BoundedBuffer<>(100);
        this.pendingJobRequests = new SynchronizedMap<>();
        this.onDisconnect = onDisconnect;
    }

    public int getMaxMemoryCapacity() {
        return maxMemoryCapacity;
    }

    public int getCurrentEstimatedMemoryUsage() {
        return this.pendingJobRequests.sumValues(JobRequest::getMemoryNeeded);
    }

    public int getEstimatedFreeMemory() {
        return this.maxMemoryCapacity - getCurrentEstimatedMemoryUsage();
    }

    public void queue(JobRequest jobRequest) {
        try {
            queuedJobRequests.put(jobRequest);
        } catch (InterruptedException ignored) {
        }
    }

    private void runRead() {
        this.running = true;

        try {
            while (running) {
                SerializeInput input = new SerializeInput(new DataInputStream(socket.getInputStream()));
                JobResult jobResult = frost.readSerializable(JobResult.class, input);

                logger.info("Received job result: " + jobResult.getJobId() + " " + new String(jobResult.getData()));

                JobRequest jobRequest = pendingJobRequests.remove(jobResult.getJobId());
                if (jobRequest == null) {
                    logger.warn("Not pending job request: " + jobResult.getJobId() + ". Race condition?");
                }

                handleJobResult.accept(jobResult);
            }
        } catch (IOException e) {
            disconnect();
        } catch (SerializationException e) {
            logger.error("Failed to deserialize job result from worker", e);
        }
    }

    private void runWrite() {
        this.running = true;

        try {
            while (running) {
                JobRequest jobRequest = queuedJobRequests.take();
                pendingJobRequests.put(jobRequest.getJobId(), jobRequest);

                logger.info("Sending job request with id " + jobRequest.getJobId() + " and " + jobRequest.getData().length + " bytes of data");

                SerializeOutput output = new SerializeOutput(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));

                frost.writeSerializable(jobRequest, JobRequest.class, output);
                frost.flush(output);
            }
        } catch (IOException e) {
            disconnect();
        } catch (SerializationException e) {
            logger.error("Failed to serialize job request to worker", e);
        } catch (InterruptedException ignored) {
        }
    }

    private void disconnect() {
        logger.info("Worker disconnected");
        this.running = false;
        this.onDisconnect.accept(this);
        stop();
    }

    public void stop() {
        this.running = false;

        this.readThread.interrupt();
        this.writeThread.interrupt();

        this.onDisconnect.accept(this);
    }

    public void run() {
        try {
            SerializeInput input = new SerializeInput(new DataInputStream(new BufferedInputStream(socket.getInputStream())));
            WSHandshakePacket wsHandshakePacket = frost.readSerializable(WSHandshakePacket.class, input);
            logger.info("Worker connected with max memory capacity of " + wsHandshakePacket.getMaxMemoryCapacity());

            this.maxMemoryCapacity = wsHandshakePacket.getMaxMemoryCapacity();

            this.readThread = new Thread(this::runRead, Thread.currentThread().getName() + "-read");
            this.writeThread = new Thread(this::runWrite, Thread.currentThread().getName() + "-write");

            this.readThread.start();
            this.writeThread.start();
        } catch (SerializationException e) {
            this.logger.error("Failed to deserialize WSHandshakePacket: ", e);
        } catch (IOException e) {
            this.logger.error("Failed to getInputStream: ", e);
        }
    }
}
