package sd.cloudcomputing.common;

public class JobResult {

    private final ResultType resultType;
    private final byte[] data;
    private final int errorCode;
    private final String errorMessage;

    public enum ResultType {
        SUCCESS,
        FAILURE
    }

    JobResult(ResultType resultType, byte[] data, int errorCode, String errorMessage) {
        this.resultType = resultType;
        this.data = data;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static JobResult success(byte[] data) {
        return new JobResult(ResultType.SUCCESS, data, -1, null);
    }

    public static JobResult failure(int errorCode, String errorMessage) {
        return new JobResult(ResultType.FAILURE, null, errorCode, errorMessage);
    }

    public ResultType getResultType() {
        return resultType;
    }

    public byte[] getData() {
        return data;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}