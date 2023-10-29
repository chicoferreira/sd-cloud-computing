package sd.cloudcomputing.common.logging.impl;

import org.jline.reader.LineReader;
import sd.cloudcomputing.common.logging.AbstractLogger;
import sd.cloudcomputing.common.logging.Console;
import sd.cloudcomputing.common.logging.LoggerFormat;

import java.io.IOException;

public class JLineConsole extends AbstractLogger implements Console {

    private final LineReader reader;
    private final String prompt;

    public JLineConsole(LoggerFormat loggerFormat, LineReader reader, String prompt) {
        super(loggerFormat);
        this.reader = reader;
        this.prompt = prompt;
    }

    @Override
    public String readInput() {
        return reader.readLine(prompt);
    }

    @Override
    public void close() throws IOException {
        reader.getTerminal().close();
    }

    @Override
    protected void print(String message) {
        reader.printAbove(message);
    }
}
