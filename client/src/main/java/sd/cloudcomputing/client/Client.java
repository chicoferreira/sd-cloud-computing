package sd.cloudcomputing.client;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import sd.cloudcomputing.client.command.Command;
import sd.cloudcomputing.client.command.CommandManager;
import sd.cloudcomputing.common.logging.Console;
import sd.cloudcomputing.common.logging.impl.DefaultLoggerFormat;
import sd.cloudcomputing.common.logging.impl.JLineConsole;
import sd.cloudcomputing.common.protocol.CSAuthPacket;
import sd.cloudcomputing.common.protocol.SCAuthResult;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

public class Client {

    private final CommandManager commandManager;
    private final Frost frost;
    private Console console;
    private boolean running;
    private ServerConnection currentConnection;

    public Client(Frost frost) {
        this.frost = frost;
        this.commandManager = new CommandManager(this);
    }

    public void run() {
        try {
            this.running = true;
            this.console = createConsole();

            Thread inputThread = new Thread(this::runCli, "Input-Thread");
            inputThread.start();

            inputThread.join();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        this.running = false;
        console.info("Client stopped");
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

        ServerConnection serverConnection = new ServerConnection(console);

        if (serverConnection.connect(ip, port)) {
            String username = console.readInput("Insert your username: ");
            String password = console.readPassword("Insert your password: ");

            CSAuthPacket authPacket = new CSAuthPacket(username, password);

            try {
                SerializeOutput serializeOutput = serverConnection.writeEnd();

                this.frost.writeSerializable(authPacket, CSAuthPacket.class, serializeOutput);
                this.frost.flush(serializeOutput);

                SerializeInput serializeInput = serverConnection.readEnd();

                SCAuthResult scAuthResult = this.frost.readSerializable(SCAuthResult.class, serializeInput);

                if (scAuthResult.isSuccess()) {
                    console.info("Successfully authenticated as " + username + " (" + scAuthResult.result() + ")");
                    Thread serverConnectionThread = new Thread(serverConnection::run, "Server-Connection-Thread");
                    serverConnectionThread.start();
                    this.currentConnection = serverConnection;
                } else {
                    console.info("Failed to authenticate: " + scAuthResult.result());
                    serverConnection.close();
                }
            } catch (IOException | SerializationException e) {
                this.console.error("Error connecting to server: " + e.getMessage());
            }
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
                String line = console.readInput("client>");
                handleCommand(line);
            } catch (EndOfFileException | UserInterruptException ignored) {
                stop();
                return;
            }
        }
    }

    private void handleCommand(String rawCommand) {
        String[] split = rawCommand.split(" ");
        if (split.length == 0) {
            return;
        }

        String commandName = split[0];
        String[] args = new String[split.length - 1];
        System.arraycopy(split, 1, args, 0, args.length);

        Command command = commandManager.getCommand(commandName);
        if (command == null) {
            console.info("Unknown command: " + commandName);
            return;
        }

        command.execute(this.console, args);
    }

    private Console createConsole() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .jansi(true)
                .dumb(true)
                .build();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        return new JLineConsole(new DefaultLoggerFormat(), reader);
    }
}
