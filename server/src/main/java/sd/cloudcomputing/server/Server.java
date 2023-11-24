package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedList;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.logging.impl.StdoutLogger;
import sd.cloudcomputing.common.logging.impl.ThreadPrefixedLoggerFormat;
import sd.cloudcomputing.common.protocol.GenericPacket;
import sd.cloudcomputing.common.protocol.SCJobNotEnoughMemoryPacket;
import sd.cloudcomputing.common.serialization.Frost;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class Server {

    private final Frost frost;
    private final Logger logger;
    private final ConnectedWorkerManager connectedWorkerManager;

    private final ClientConnectionManager clientConnectionManager;
    private final ClientManager clientManager;
    private final ClientPacketHandler clientPacketHandler;

    private final JobMappingService jobMappingService;

    private boolean running;

    private Thread workerConnectionHandler;
    private Thread clientConnectionHandler;

    public Server(Frost frost) {
        this.frost = frost;
        this.logger = new StdoutLogger(new ThreadPrefixedLoggerFormat());
        this.clientConnectionManager = new ClientConnectionManager();
        this.clientManager = new ClientManager();
        this.connectedWorkerManager = new ConnectedWorkerManager();
        this.jobMappingService = new JobMappingService();
        this.clientPacketHandler = new ClientPacketHandler(this.logger, this.connectedWorkerManager, this);
    }

    public void run(int serverPort, int workerConnectionServerPort) {
        addShutdownHook();

        this.running = true;
        this.workerConnectionHandler = new Thread(() -> runWorkerConnectionHandler(workerConnectionServerPort), "Worker-Connection-Handler-Thread");
        this.workerConnectionHandler.start();

        this.clientConnectionHandler = new Thread(() -> runClientConnectionHandler(serverPort), "Client-Connection-Handler-Thread");
        this.clientConnectionHandler.start();

        try {
            this.workerConnectionHandler.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        stop();
    }

    public void queueClientJobRequest(Client client, ClientConnection clientConnection, JobRequest clientJobRequest) {
        JobRequest serverJobRequest = this.jobMappingService.mapClientRequestToServerRequest(client, clientJobRequest);
        queueServerJobRequest(client, clientConnection, clientJobRequest.jobId(), serverJobRequest);
    }

    private void queueServerJobRequest(Client client, ClientConnection clientConnection, int clientJobId, JobRequest serverJobRequest) {
        logger.info("Scheduling job request with id (client: " + clientJobId + " server: " + serverJobRequest.jobId() + ") " +
                "and " + serverJobRequest.data().length + " bytes of data from " + client.getName());

        if (!connectedWorkerManager.scheduleJob(serverJobRequest)) {
            this.jobMappingService.deleteMapping(serverJobRequest.jobId());

            logger.warn("No memory for " + clientJobId + " from " + client.getName() + " with " + serverJobRequest.memoryNeeded() + " memory needed");
            SCJobNotEnoughMemoryPacket notEnoughMemoryPacket = new SCJobNotEnoughMemoryPacket(clientJobId);
            clientConnection.enqueuePacket(new GenericPacket(SCJobNotEnoughMemoryPacket.PACKET_ID, notEnoughMemoryPacket));
        }
    }

    /**
     * Reschedules the given job requests.
     * Executed when a worker disconnects when it has pending jobs.
     */
    public void rescheduleJobs(List<JobRequest> pendingJobRequests) {
        for (JobRequest serverJobRequest : pendingJobRequests) {
            JobMappingService.Mapping mapping = jobMappingService.getMappingFromServerJobId(serverJobRequest.jobId());
            if (mapping == null) return;

            Client client = mapping.client();
            ClientConnection clientConnection = this.clientConnectionManager.getClientConnection(client);
            if (clientConnection == null) {
                this.logger.warn("Client " + client.getName() + " disconnected, no need to reschedule " + serverJobRequest.jobId());
                continue;
            }

            queueServerJobRequest(client, clientConnection, mapping.clientJobId(), serverJobRequest);
        }
    }

    public void queueJobResultToClient(JobResult serverJobResult) {
        int serverJobId = serverJobResult.jobId();

        JobMappingService.Mapping mapping = this.jobMappingService.retrieveMappingFromServerJobId(serverJobId);
        if (mapping == null) {
            this.logger.error("Race condition on job result with id " + serverJobId + ". No client or job result mapping found for this job id");
            return;
        }

        Client client = mapping.client();
        int clientJobId = mapping.clientJobId();

        JobResult clientJobResult = serverJobResult.mapId(clientJobId);

        ClientConnection clientConnection = this.clientConnectionManager.getClientConnection(client);
        if (clientConnection == null) { // requirements have not specified what should happen in this case, so we will just ignore the result
            this.logger.warn("Client " + client.getName() + " disconnected before receiving job result " + clientJobId);
            return;
        }

        clientConnection.enqueuePacket(new GenericPacket(JobResult.PACKET_ID, clientJobResult));
    }

    /**
     * Listens for worker connections and handles them
     *
     * @param workerConnectionServerPort port to listen for worker connections
     */
    private void runWorkerConnectionHandler(int workerConnectionServerPort) {
        try (ServerSocket serverSocket = new ServerSocket(workerConnectionServerPort)) {
            logger.info("Listening for worker connections on port " + workerConnectionServerPort + "...");
            SynchronizedList<Thread> pendingConnections = new SynchronizedList<>();
            int currentWorkerId = 0;
            while (this.running) {
                try {
                    Socket workerSocket = serverSocket.accept();
                    logger.info("Received connection from worker " + workerSocket.getInetAddress() + ":" + workerSocket.getPort() + ".");
                    Thread connectionThread = new Thread(() -> {
                        WorkerConnection workerConnection = new WorkerConnection(this.logger, this.frost, workerSocket, this, connectedWorkerManager);
                        if (workerConnection.start()) {
                            this.connectedWorkerManager.addConnectedWorker(workerConnection);
                        }
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

    // TODO: deduplicar este método com o de cima
    private void runClientConnectionHandler(int clientServerPort) {
        try (ServerSocket serverSocket = new ServerSocket(clientServerPort)) {
            logger.info("Listening for client connections on port " + clientServerPort + "...");
            SynchronizedList<Thread> pendingConnections = new SynchronizedList<>();
            int currentConnectionId = 0;
            while (this.running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Received connection from client " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + ".");
                    Thread connectionThread = new Thread(() -> {
                        ClientConnection clientConnection = new ClientConnection(this.logger, this.frost, this.clientManager, clientSocket, clientConnectionManager, this.clientPacketHandler);
                        clientConnection.start();
                    }, "Client-Connection-Thread-" + currentConnectionId++); // Only this thread increments the id, no need to synchronize

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
            logger.error("Failed to listen server at port " + clientServerPort, e);
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            stop();
        }));
    }

    public void stop() {
        this.running = false;
        this.connectedWorkerManager.closeAll();
        this.workerConnectionHandler.interrupt();
        this.clientConnectionManager.disconnectAll();
        this.clientConnectionHandler.interrupt();
    }

}
