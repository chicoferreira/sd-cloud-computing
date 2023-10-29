package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Worker {

    private final WorkerScheduler workerScheduler;
    private final BoundedBuffer<JobResult> queuedJobResults;

    private boolean running;
    private ServerSocket serverSocket;

    private Thread readerThread;
    private Thread jobResultHandlerThread;

    public Worker(int maxMemoryCapacity, int maxConcurrentJobs) {
        JobExecutor jobExecutor = new JobExecutor();
        this.workerScheduler = new WorkerScheduler(jobExecutor, maxMemoryCapacity, maxConcurrentJobs, this::receiveJobResult);

        this.queuedJobResults = new BoundedBuffer<>(100);
    }

    public void start(int port) {
        initServer(port);
        setupShutdownHook();

        this.readerThread = new Thread(this::runServerLoop, "Worker-Server-Thread");
        readerThread.start();

        this.jobResultHandlerThread = new Thread(this::handleJobResults, "Worker-Job-Result-Handler-Thread");
        jobResultHandlerThread.start();

        this.workerScheduler.start();

        stop();
    }

    private void initServer(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Worker listening on port " + port);
            this.serverSocket = serverSocket;
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void runServerLoop() {
        running = true;
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Server connected. Waiting for requests...");

                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                Object object = objectInputStream.readObject();

                if (object instanceof JobRequest jobRequest) {
                    this.workerScheduler.queue(jobRequest);
                } else {
                    System.err.println("Received object is not a byte array");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveJobResult(JobResult jobResult) {
        try {
            queuedJobResults.put(jobResult);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleJobResults() {
        while (running) {
            try {
                JobResult jobResult = queuedJobResults.take();
                if (jobResult.getResultType() == JobResult.ResultType.FAILURE) {
                    System.out.println("Job failed with error code " + jobResult.getErrorCode() + ": " + jobResult.getErrorMessage());
                    return;
                }

                System.out.println("Job succeeded with result: " + jobResult.getData().length + " bytes");

                // TODO: send result back to server
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void stop() {
        try {
            readerThread.join();
            jobResultHandlerThread.join();

            this.workerScheduler.stop();
            serverSocket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down worker...");
            running = false;
        }));
    }
}
