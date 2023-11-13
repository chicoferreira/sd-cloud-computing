package sd.cloudcomputing.server;

import sd.cloudcomputing.common.concurrent.SynchronizedList;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.logging.impl.StdoutLogger;
import sd.cloudcomputing.common.logging.impl.ThreadPrefixedLoggerFormat;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Server {

    private final Frost frost;
    private final Logger logger;

    private boolean running;

    private final SynchronizedList<WorkerConnection> workerConnections;
    private final SynchronizedList<ClientConnection> clientConnections;

    private Thread workerConnectionHandler;
    private final ClientManager clientManager;
    private Thread clientConnectionHandler;

    public Server(Frost frost) {
        this.frost = frost;
        this.logger = new StdoutLogger(new ThreadPrefixedLoggerFormat());
        this.workerConnections = new SynchronizedList<>();
        this.clientConnections = new SynchronizedList<>();
        this.clientManager = new ClientManager();
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

    public WorkerConnection findAvailableWorker(int memoryNeeded) {
        workerConnections.internalLock();
        try {
            List<WorkerConnection> internalList = workerConnections.getInternalList();
            Optional<WorkerConnection> first = internalList.stream()
                    .sorted(Comparator.comparingInt(WorkerConnection::getEstimatedFreeMemory))
                    .filter(workerConnection -> workerConnection.getMaxMemoryCapacity() >= memoryNeeded)
                    .findFirst();

            return first.orElse(null);
        } finally {
            workerConnections.internalUnlock();
        }
    }

//    public void queueJob(JobRequest jobRequest) {
//        WorkerConnection worker = findAvailableWorker(jobRequest.getMemoryNeeded());
//        if (worker == null) {
//            logger.info("No worker has the memory needed for " + jobRequest.getJobId() + ".");
//            // TODO: avisar o cliente que não há workers que possam executar o job
//            return;
//        }
//        worker.queue(jobRequest);
//    }

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
                    logger.info("Received connection from worker " + workerSocket.getInetAddress() + ":" + workerSocket.getPort() + ". Waiting for handshake...");
                    Thread connectionThread = new Thread(() -> {
                        WorkerConnection workerConnection = new WorkerConnection(this.logger, this.frost, workerSocket);
                        try {
                            workerConnection.performHandshake();
                        } catch (SerializationException e) {
                            logger.error("Error deserializing handshake packet: ", e);
                        } catch (IOException e) {
                            logger.error("Error receiving packet to worker: ", e);
                        }

                        this.workerConnections.add(workerConnection);
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
                    logger.info("Received connection from client " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + ". Waiting for authentication...");
                    Thread connectionThread = new Thread(() -> {
                        try {
                            ClientConnection clientConnection = new ClientConnection(this.logger, this.frost, this.clientManager, clientSocket);
                            Client client = clientConnection.acceptLogin();
                            if (client != null) {
                                this.logger.info("Client " + client + " authenticated successfully");
                                clientConnection.startReadWrite();

                                this.clientConnections.add(clientConnection);
                            }
                        } catch (IOException e) {
                            this.logger.error("Error sending packet to client: " + e.getMessage());
                        } catch (SerializationException e) {
                            this.logger.error("Serialization error auth packet: " + e.getMessage());
                        }
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
        this.workerConnections.forEach(WorkerConnection::close);
        this.workerConnectionHandler.interrupt();
        this.clientConnections.forEach(ClientConnection::close);
        this.clientConnectionHandler.interrupt();
    }

}
