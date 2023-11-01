package sd.cloudcomputing.common.logging.impl;

import sd.cloudcomputing.common.logging.AbstractLogger;
import sd.cloudcomputing.common.logging.LoggerFormat;

import java.io.PrintStream;

public class StdoutLogger extends AbstractLogger {

    private final PrintStream out;
    private final PrintStream err;

    public StdoutLogger(LoggerFormat loggerFormat) {
        super(loggerFormat);

        /* We store the original System.out and System.err because of the hooks methods in the super class
          which changes the System.out and System.err to print to the logger */
        this.out = System.out;
        this.err = System.err;
    }

    @Override
    protected void print(String message) {
        this.out.println(message);
    }

    @Override
    protected void printError(String message) {
        this.err.println(message);
    }
}
