package sd.cloudcomputing.common.logging.impl;

import sd.cloudcomputing.common.logging.LoggerFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DefaultLoggerFormat implements LoggerFormat {

    @Override
    public String info(String message) {
        return getTimestampString() + " [INFO] " + message;
    }

    @Override
    public String warn(String message) {
        return getTimestampString() + " [WARN] " + message;
    }

    @Override
    public String error(String message) {
        return getTimestampString() + " [ERROR] " + message;
    }

    @Override
    public String error(String message, Throwable throwable) {
        return getTimestampString() + " [ERROR] " + message + "\n" + throwable.getMessage();
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String getTimestampString() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
