package sd.cloudcomputing.common.logging;

import java.io.PrintStream;

public abstract class AbstractLogger implements Logger {

    private final LoggerFormat loggerFormat;

    public AbstractLogger(LoggerFormat loggerFormat) {
        this.loggerFormat = loggerFormat;
    }

    @Override
    public final void info(String message) {
        print(loggerFormat.format("INFO", message, null));
    }

    @Override
    public final void warn(String message) {
        print(loggerFormat.format("WARN", message, null));
    }

    @Override
    public final void error(String message) {
        printError(loggerFormat.format("ERROR", message, null));
    }

    @Override
    public final void error(String message, Throwable throwable) {
        printError(loggerFormat.format("ERROR", message, throwable));
        throwable.printStackTrace();
    }

    public final void hookSystemPrint() {
        System.setOut(new PrintStream(System.out) {
            @Override
            public void println(String x) {
                info(x);
            }
        });
        System.setErr(new PrintStream(System.err) {
            @Override
            public void println(String x) {
                error(x);
            }
        });
    }

    protected abstract void print(String message);

    protected void printError(String message) {
        print(message);
    }
}
