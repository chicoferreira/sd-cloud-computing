package sd.cloudcomputing.common;

import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

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

    public static class Serialization implements Serialize<JobResult> {

        @Override
        public JobResult deserialize(SerializeInput input, Frost frost) {
            ResultType type = frost.readBoolean(input) ? ResultType.SUCCESS : ResultType.FAILURE;
            return switch (type) {
                case FAILURE -> {
                    String errorMessage = frost.readString(input);
                    int errorCode = frost.readInt(input);
                    yield JobResult.failure(errorCode, errorMessage);
                }
                case SUCCESS -> JobResult.success(frost.readBytes(input));
            };
        }

        @Override
        public void serialize(JobResult object, SerializeOutput output, Frost frost) {
            frost.writeBoolean(object.getResultType() == ResultType.SUCCESS, output);
            switch (object.getResultType()) {
                case FAILURE -> {
                    frost.writeString(object.getErrorMessage(), output);
                    frost.writeInt(object.getErrorCode(), output);
                }
                case SUCCESS -> frost.writeBytes(object.getData(), output);
            }
        }
    }
}