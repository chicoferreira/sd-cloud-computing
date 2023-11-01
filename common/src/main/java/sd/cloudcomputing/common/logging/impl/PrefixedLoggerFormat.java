package sd.cloudcomputing.common.logging.impl;

import sd.cloudcomputing.common.logging.LoggerFormat;

public abstract class PrefixedLoggerFormat implements LoggerFormat {

    @Override
    public String format(String level, String message, Throwable throwable) {
        return getPrefix(level) + " " + message + (throwable != null ? throwable.getMessage() : "");
    }

    protected abstract String getPrefix(String level);

}
