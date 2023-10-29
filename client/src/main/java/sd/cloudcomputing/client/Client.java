package sd.cloudcomputing.client;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import sd.cloudcomputing.common.logging.Console;
import sd.cloudcomputing.common.logging.impl.DefaultLoggerFormat;
import sd.cloudcomputing.common.logging.impl.JLineConsole;

import java.io.IOException;

public class Client {

    private Console console;

    public void run() {
        try {
            this.console = createConsole();
            while (true) {
                try {
                    String line = console.readInput();
                    console.info("Read: " + line);
                } catch (EndOfFileException | UserInterruptException ignored) {
                    stop();
                    return;
                }
            }
        } catch (IOException e) {
            console.error("Failed to setup console", e);
        }
    }

    public void stop() {
        console.info("Client stopped");
        try {
            console.close();
        } catch (IOException e) {
            console.error("Failed to close console", e);
        }
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
