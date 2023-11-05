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

import java.io.IOException;

public class Client {

    private final CommandManager commandManager;
    private Console console;
    private boolean running;
    private ServerConnection serverConnection;
    private Thread serverConnectionThread;

    public Client() {
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

        this.serverConnection = new ServerConnection(console);

        if (this.serverConnection.connect(ip, port)) {
            this.serverConnectionThread = new Thread(() -> serverConnection.run(), "Server-Connection-Thread");
            this.serverConnectionThread.start();
        }
    }

    public ServerConnection getCurrentServerConnection() {
        return serverConnection;
    }

    public boolean isConnected() {
        return serverConnection != null && serverConnection.isConnected();
    }

    private void runCli() {
        while (this.running) {
            try {
                String line = console.readInput();
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

        return new JLineConsole(new DefaultLoggerFormat(), reader, "client> ");
    }
}
