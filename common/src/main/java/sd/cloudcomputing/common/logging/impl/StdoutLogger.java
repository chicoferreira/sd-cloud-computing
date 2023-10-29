package sd.cloudcomputing.common.logging.impl;

import sd.cloudcomputing.common.logging.AbstractLogger;
import sd.cloudcomputing.common.logging.LoggerFormat;

public class StdoutLogger extends AbstractLogger {

    public StdoutLogger(LoggerFormat loggerFormat) {
        super(loggerFormat);
    }

    @Override
    protected void print(String message) {
        System.out.println(message);
    }

    @Override
    protected void printError(String message) {
        System.err.println(message);
    }
}
