package sd.cloudcomputing.client;

import org.jetbrains.annotations.Nullable;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import sd.cloudcomputing.client.command.CommandManager;
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
    private Console console;
    private boolean running;
    private ServerConnection currentConnection;

    public Application(Frost frost) {
        this.frost = frost;
        this.commandManager = new CommandManager(this);
    }

    public void run() {
        try {
            this.running = true;
            this.console = createConsole();

            this.runCli();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        this.running = false;
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
                stop();
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
}
