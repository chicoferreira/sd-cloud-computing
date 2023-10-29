package sd.cloudcomputing.common.serialization;

public class SerializationException extends RuntimeException {

    public SerializationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
