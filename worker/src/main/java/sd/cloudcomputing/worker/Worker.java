package sd.cloudcomputing.worker;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class Worker {

    private final ExecutorService executorService;
    private boolean running;
    private ServerSocket serverSocket;

    public Worker(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void start(int port) {
        initServer(port);
        setupShutdownHook();
        runServerLoop();
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
                    CompletableFuture.supplyAsync(new JobExecutor(jobRequest), executorService).thenAccept(this::handleJobResult);
                } else {
                    System.err.println("Received object is not a byte array");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleJobResult(JobResult jobResult) {
        if (jobResult.getResultType() == JobResult.ResultType.FAILURE) {
            System.out.println("Job failed with error code " + jobResult.getErrorCode() + ": " + jobResult.getErrorMessage());
            return;
        }

        System.out.println("Job succeeded with result: " + jobResult.getData().length + " bytes");

        // TODO: send result back to server
    }

    private void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
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
