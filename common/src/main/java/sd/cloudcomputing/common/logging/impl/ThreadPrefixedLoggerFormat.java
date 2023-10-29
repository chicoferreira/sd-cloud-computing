package sd.cloudcomputing.common.logging.impl;

public class ThreadPrefixedLoggerFormat extends DefaultLoggerFormat {

    @Override
    protected String getPrefix(String level) {
        return super.getPrefix(level) + " [" + Thread.currentThread().getName() + "]";
    }
}
