package sd.cloudcomputing.client;

import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import sd.cloudcomputing.client.command.CommandManager;
import sd.cloudcomputing.client.job.ClientJob;
import sd.cloudcomputing.client.job.JobManager;
import sd.cloudcomputing.client.job.JobResultFileWorker;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.logging.Console;
import sd.cloudcomputing.common.logging.impl.DefaultLoggerFormat;
import sd.cloudcomputing.common.logging.impl.JLineConsole;
import sd.cloudcomputing.common.serialization.Frost;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Supplier;

public class Application {

    private final CommandManager commandManager;
    private final Frost frost;
    private final Console console;
    private final JobResultFileWorker jobResultFileWorker;
    private final JobManager jobManager;

    private boolean running;
    private ServerConnection currentConnection;

    public Application(Frost frost) throws IOException {
        this.frost = frost;
        this.commandManager = new CommandManager(this);

        this.console = createConsole();
        this.jobResultFileWorker = new JobResultFileWorker(this.console);
        this.jobManager = new JobManager();
    }

    public void run() {
        this.running = true;

        this.jobResultFileWorker.run();
        this.runCli();

        stop();
    }

    private void stop() {
        this.running = false;

        if (this.currentConnection != null) {
            this.currentConnection.disconnect();
        }

        this.jobResultFileWorker.stop();

        console.info("Application stopped");
        try {
            console.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect(String ip, int port) {
        if (isConnected()) {
            return;
        }

        try {
            Socket socket = new Socket(ip, port);
            ServerConnection serverConnection = new ServerConnection(console, frost, this, socket);

            console.info("Connected to server at " + serverConnection.getSocket().getAddressWithPort());

            Supplier<String> username = () -> console.readInput("Insert your username: ");
            Supplier<String> password = () -> console.readPassword("Insert your password: ");

            if (serverConnection.start(username, password)) {
                this.currentConnection = serverConnection;
            }
        } catch (IOException e) {
            console.error("Failed to connect to server: " + e.getMessage());
        }
    }

    public @Nullable ServerConnection getCurrentServerConnection() {
        return currentConnection == null || !currentConnection.isConnected() ? null : currentConnection;
    }

    public boolean isConnected() {
        return getCurrentServerConnection() != null;
    }

    private void runCli() {
        while (this.running) {
            try {
                String line = console.readInput("client> ");
                this.commandManager.handleCommand(console, line);
            } catch (EndOfFileException | UserInterruptException ignored) {
                return;
            }
        }
    }

    private Console createConsole() throws IOException {
        Terminal terminal = TerminalBuilder.builder().jansi(true).dumb(true).build();

        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

        return new JLineConsole(new DefaultLoggerFormat(), reader);
    }

    public void notifyServerDisconnect() {
        this.currentConnection = null;
    }

    public int createAndScheduleJobRequest(byte[] bytes, int neededMemory, @Nullable String outputFileName) {
        if (!this.isConnected()) {
            return -1;
        }

        int jobId = this.currentConnection.scheduleJob(bytes, neededMemory);

        if (outputFileName == null) {
            outputFileName = getDefaultFileName(jobId);
        }

        ClientJob clientJob = new ClientJob.Scheduled(jobId, outputFileName, neededMemory, System.currentTimeMillis());
        this.jobManager.addJob(clientJob);
        return jobId;
    }

    private String getDefaultFileName(int jobId) {
        return "job-" + jobId + ".7z"; // The JobFunction will create bytes of a .7z file on success
    }

    public void notifyJobResult(JobResult jobResult) {
        switch (jobResult) {
            case JobResult.Success success -> {
                console.info("Job " + success.jobId() + " completed successfully with " + success.data().length + " bytes.");

                ClientJob.Finished clientJob = this.jobManager.registerJobResult(success.jobId(), success);
                String outputFile = clientJob == null ? getDefaultFileName(jobResult.jobId()) : clientJob.jobOutputFile();
                try {
                    this.jobResultFileWorker.queueWrite(success, outputFile);
                } catch (InterruptedException e) {
                    console.error("Failed to queue job result for job " + success.jobId() + ": " + e.getMessage());
                }
            }
            case JobResult.Failure failure ->
                    console.error("Job " + failure.jobId() + " failed with error code " + failure.errorCode() + ": " + failure.errorMessage());
        }
    }

    public void exit() {
        this.running = false;
    }
}
