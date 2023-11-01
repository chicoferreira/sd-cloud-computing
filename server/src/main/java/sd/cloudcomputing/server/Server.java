package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedList;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.logging.impl.StdoutLogger;
import sd.cloudcomputing.common.logging.impl.ThreadPrefixedLoggerFormat;
import sd.cloudcomputing.common.serialization.Frost;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

    private final Frost frost;
    private final Logger logger;

    private boolean running;

    private final SynchronizedList<WorkerConnection> workerConnections;

    private Thread workerConnectionHandler;

    public Server(Frost frost) {
        this.frost = frost;
        this.logger = new StdoutLogger(new ThreadPrefixedLoggerFormat());
        this.workerConnections = new SynchronizedList<>();
    }

    public void run(int serverPort, int workerConnectionServerPort) {
        addShutdownHook();

        this.running = true;
        this.workerConnectionHandler = new Thread(() -> runWorkerConnectionHandler(workerConnectionServerPort), "Worker-Connection-Handler-Thread");
        workerConnectionHandler.start();

        new Thread(() -> {
            while (true) {
                workerConnections.forEach(workerConnection -> {
                    for (int i = 0; i < 100; i++) {
                        try {
                            workerConnection.queue(new JobRequest(i, "Hello World!".getBytes(), 100));
                        } catch (InterruptedException ignored) {
                        }
                    }
                });
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();

        try {
            workerConnectionHandler.join();
        } catch (InterruptedException ignored) {
        }

        stop();

//            Socket workerSocket = serverSocket.accept();
//            OutputStream outputStream = workerSocket.getOutputStream();
//
//            Frost frost = new Frost();
//            frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
//            frost.registerSerializer(JobResult.class, new JobResult.Serialization());
//
//            SerializeOutput serializeOutput = new SerializeOutput(new DataOutputStream(new BufferedOutputStream(outputStream)));
//
//            for (int i = 0; i < 100; i++) {
//                JobRequest jobRequest = new JobRequest(i, "Hello World!".getBytes(), 100);
//                frost.writeSerializable(jobRequest, JobRequest.class, serializeOutput);
//                frost.flush(serializeOutput);
//                try {
//                    Thread.sleep(10); // TODO: unsure why this is needed
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            for (int i = 0; i < 100; i++) {
//                InputStream inputStream = workerSocket.getInputStream();
//                SerializeInput serializeInput = new SerializeInput(new DataInputStream(new BufferedInputStream(inputStream)));
//                JobResult receivedJobResult = frost.readSerializable(JobResult.class, serializeInput);
//                System.out.println("Received job result: " + receivedJobResult.getJobId() + " " + Arrays.toString(receivedJobResult.getData()));
//            }
    }

    public void runWorkerConnectionHandler(int workerConnectionServerPort) {
        try (ServerSocket serverSocket = new ServerSocket(workerConnectionServerPort)) {
            logger.info("Listening for worker connections on port " + workerConnectionServerPort + "...");
            SynchronizedList<Thread> pendingConnections = new SynchronizedList<>();
            int currentWorkerId = 0;
            while (this.running) {
                try {
                    Socket workerSocket = serverSocket.accept();
                    logger.info("Received connection from worker " + workerSocket.getInetAddress() + ":" + workerSocket.getPort() + ". Waiting for handshake...");
                    Thread connectionThread = new Thread(() -> {
                        WorkerConnection workerConnection = new WorkerConnection(this.logger, this.frost, workerSocket, this::handleJobResult);
                        workerConnection.run();

                        this.workerConnections.add(workerConnection);
                        pendingConnections.remove(Thread.currentThread());
                    }, "Worker-Connection-Thread-" + currentWorkerId++); // Only this thread increments the worker id, no need to synchronize

                    pendingConnections.add(connectionThread);

                    connectionThread.start();
                } catch (SocketException e) {
                    logger.error("Socket error", e);
                    this.running = false;
                }
            }

            logger.info("Interrupting pending connections...");
            pendingConnections.forEach(Thread::interrupt);
        } catch (IOException e) {
            logger.error("Failed to listen server at port " + workerConnectionServerPort, e);
        }
    }

    private void handleJobResult(JobResult jobResult) {
        logger.info("Sending job result back to client: " + jobResult.getJobId() + " " + new String(jobResult.getData()));

        // TODO: send job result back to client
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            stop();
        }));
    }

    private void stop() {
        this.running = false;
        this.workerConnections.forEach(WorkerConnection::stop);
        this.workerConnectionHandler.interrupt();
    }

}
