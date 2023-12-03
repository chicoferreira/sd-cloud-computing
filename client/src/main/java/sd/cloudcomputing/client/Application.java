package sd.cloudcomputing.client;

import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import sd.cloudcomputing.client.api.*;
import sd.cloudcomputing.client.command.CommandManager;
import sd.cloudcomputing.common.logging.Console;
import sd.cloudcomputing.common.logging.impl.DefaultLoggerFormat;
import sd.cloudcomputing.common.logging.impl.JLineConsole;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.io.IOException;
import java.util.List;

public class Application {

    private final Client client;
    private final CommandManager commandManager;
    private final Console console;
    private final JobResultFileWorker jobResultFileWorker;
    private final ServerSessionListener listener;

    private boolean running;
    private ServerSession currentSession;

    public Application() throws IOException {
        this.client = Client.createNewClient();
        this.commandManager = new CommandManager(this);
        this.console = createConsole();

        this.jobResultFileWorker = new JobResultFileWorker(this.console);

        this.listener = new ServerSessionListenerImpl(this, this.console, this.jobResultFileWorker);
    }

    public void run() {
        this.running = true;

        this.jobResultFileWorker.run();
        this.runCli();

        stop();
    }

    private void stop() {
        this.running = false;

        if (this.currentSession != null) {
            this.currentSession.disconnect();
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
            ServerNoAuthSession noAuthSession = this.client.connect(ip, port);
            console.info("Connected to server at " + noAuthSession.getAddressWithPort());

            String username = console.readInput("Insert your username: ");
            String password = console.readPassword("Insert your password: ");

            AuthenticateResult authenticateResult = noAuthSession.login(username, password);
            if (!authenticateResult.isSuccess()) {
                this.console.info("Failed to authenticate: " + authenticateResult);
                noAuthSession.disconnect();
                return;
            }

            ServerSession serverConnection = noAuthSession.createLoggedSession(console, this.client, this.listener);
            this.console.info("Successfully authenticated as " + username + " (" + authenticateResult + ")");
            serverConnection.startReadWrite();

            this.currentSession = serverConnection;
        } catch (IOException e) {
            console.error("Failed to connect to server: " + e.getMessage());
        } catch (SerializationException e) {
            console.error("Failed to serialize packet: " + e.getMessage());
        }
    }

    public List<ClientJob> getAllJobs() {
        return this.client.getAllJobs();
    }

    private @Nullable ServerSession getCurrentServerSession() {
        return currentSession == null || !currentSession.isConnected() ? null : currentSession;
    }

    public boolean isConnected() {
        return getCurrentServerSession() != null;
    }

    private void runCli() {
        console.info("Type 'help' to see all available commands.");
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

    void notifyServerDisconnect() {
        console.info("Disconnected from server");
        this.currentSession = null;
    }

    public void exit() {
        this.running = false;
    }

    public int createAndScheduleJobRequest(byte[] bytes, int memoryAsInt, String outputFile) {
        ServerSession currentServerSession = getCurrentServerSession();
        if (currentServerSession == null) {
            return -1;
        }

        return this.client.createAndScheduleJobRequest(currentServerSession, bytes, memoryAsInt, outputFile);
    }

    public boolean sendServerStatusRequest() {
        ServerSession currentServerSession = getCurrentServerSession();
        if (currentServerSession != null) {
            currentServerSession.sendServerStatusRequest();
            return true;
        }

        return false;
    }

    public void disconnect() {
        ServerSession currentServerSession = this.getCurrentServerSession();
        if (currentServerSession != null) {
            currentServerSession.disconnect();
        }
    }
}
