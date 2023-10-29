package sd.cloudcomputing.common.logging;

public interface LoggerFormat {

    String info(String message);

    String warn(String message);

    String error(String message);

    String error(String message, Throwable throwable);

}
