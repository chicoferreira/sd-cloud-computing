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
    private int maxMemoryCapacity;
    private boolean running;

    private SynchronizedMap<Integer, JobRequest> pendingJobRequests;

    private final BoundedBuffer<JobRequest> queuedJobRequests;
    private Thread readThread;
    private Thread writeThread;

    public WorkerConnection(Logger logger, Frost frost, Socket socket, Consumer<JobResult> handleJobResult) {
        this.logger = logger;
        this.frost = frost;
        this.socket = socket;
        this.handleJobResult = handleJobResult;
        this.queuedJobRequests = new BoundedBuffer<>(100);
        this.pendingJobRequests = new SynchronizedMap<>();
    }

    public int getMaxMemoryCapacity() {
        return maxMemoryCapacity;
    }

    public int getCurrentEstimatedMemoryUsage() {
        return this.pendingJobRequests.sumValues(JobRequest::getMemoryNeeded);
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
            logger.info("Worker disconnected");

        } catch (SerializationException e) {
            logger.error("Failed to deserialize job result from worker", e);
        }
    }

    public void queue(JobRequest jobRequest) throws InterruptedException {
        queuedJobRequests.put(jobRequest);
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
            logger.info("Worker disconnected");
        } catch (SerializationException e) {
            logger.error("Failed to serialize job request to worker", e);
        } catch (InterruptedException ignored) {
        }
    }

    public void stop() {
        this.running = false;

        this.readThread.interrupt();
        this.writeThread.interrupt();
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

            try {
                this.readThread.join();
                this.writeThread.join();
            } catch (InterruptedException ignored) {
            }


        } catch (SerializationException e) {
            this.logger.error("Failed to deserialize WSHandshakePacket: ", e);
        } catch (IOException e) {
            this.logger.error("Failed to getInputStream: ", e);
        }
    }
}
