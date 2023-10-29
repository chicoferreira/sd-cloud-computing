package sd.cloudcomputing.common.logging;

public abstract class AbstractLogger implements Logger {

    private final LoggerFormat loggerFormat;

    public AbstractLogger(LoggerFormat loggerFormat) {
        this.loggerFormat = loggerFormat;
    }

    @Override
    public final void info(String message) {
        print(loggerFormat.info(message));
    }

    @Override
    public final void warn(String message) {
        print(loggerFormat.warn(message));
    }

    @Override
    public final void error(String message) {
        printError(loggerFormat.error(message));
    }

    @Override
    public final void error(String message, Throwable throwable) {
        printError(loggerFormat.error(message, throwable));
    }

    protected abstract void print(String message);

    protected void printError(String message) {
        print(message);
    }
}
