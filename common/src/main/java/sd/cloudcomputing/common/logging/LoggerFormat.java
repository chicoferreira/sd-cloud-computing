package sd.cloudcomputing.common.logging;

public interface LoggerFormat {

    String format(String level, String message, Throwable throwable);

}
