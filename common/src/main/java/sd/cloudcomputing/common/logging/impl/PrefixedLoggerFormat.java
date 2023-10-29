package sd.cloudcomputing.common.logging.impl;

import sd.cloudcomputing.common.logging.LoggerFormat;

import java.util.Arrays;

public abstract class PrefixedLoggerFormat implements LoggerFormat {


    @Override
    public String info(String message) {
        return getInfoPrefix() + " " + message;
    }

    @Override
    public String warn(String message) {
        return getWarnPrefix() + " " + message;
    }

    @Override
    public String error(String message) {
        return getErrorPrefix() + " " + message;
    }

    @Override
    public String error(String message, Throwable throwable) {
        return getErrorPrefix() + " " + message + throwable.getMessage() + Arrays.toString(throwable.getStackTrace());
    }

    protected abstract String getPrefix(String level);

    protected String getInfoPrefix() {
        return getPrefix("INFO");
    }

    protected String getWarnPrefix() {
        return getPrefix("WARN");
    }

    protected String getErrorPrefix() {
        return getPrefix("ERROR");
    }
}
