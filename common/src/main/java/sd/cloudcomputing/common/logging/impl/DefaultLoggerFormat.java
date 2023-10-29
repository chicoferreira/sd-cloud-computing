package sd.cloudcomputing.common.logging.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DefaultLoggerFormat extends PrefixedLoggerFormat {

    @Override
    protected String getPrefix(String level) {
        return getTimestampString() + " [" + level + "]";
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String getTimestampString() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
