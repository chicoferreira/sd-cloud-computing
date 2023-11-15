package sd.cloudcomputing.client;

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
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.io.IOException;

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
            console.info("Already connected to a server");
        }

        ServerConnection serverConnection = new ServerConnection(console, frost);

        try {
            if (serverConnection.connect(ip, port)) {
                console.info("Connected to server at " + serverConnection.getSocket().getAddressWithPort());
                String username = console.readInput("Insert your username: ");
                String password = console.readPassword("Insert your password: ");

                try {
                    AuthenticateResult result = serverConnection.login(username, password);
                    if (result.isSuccess()) {
                        console.info("Successfully authenticated as " + username + " (" + result + ")");
                        serverConnection.startReadWrite();
                        this.currentConnection = serverConnection;
                    } else {
                        console.info("Failed to authenticate: " + result);
                        serverConnection.close();
                    }
                } catch (IOException e) {
                    console.error("Error sending packet to server: " + e.getMessage());
                } catch (SerializationException e) {
                    console.error("Error serializing auth packet: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            console.error("Failed to connect to server: " + e.getMessage());
        }
    }

    public ServerConnection getCurrentServerConnection() {
        return currentConnection;
    }

    public boolean isConnected() {
        return currentConnection != null && currentConnection.isConnected();
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
}
