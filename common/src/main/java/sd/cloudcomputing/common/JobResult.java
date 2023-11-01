package sd.cloudcomputing.common;

import sd.cloudcomputing.common.serialization.*;

public class JobResult {

    private final int jobId;
    private final ResultType resultType;
    private final byte[] data;
    private final int errorCode;
    private final String errorMessage;

    public enum ResultType {
        SUCCESS, FAILURE
    }

    JobResult(int jobId, ResultType resultType, byte[] data, int errorCode, String errorMessage) {
        this.jobId = jobId;
        this.resultType = resultType;
        this.data = data;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static JobResult success(int jobId, byte[] data) {
        return new JobResult(jobId, ResultType.SUCCESS, data, -1, null);
    }

    public static JobResult failure(int jobId, int errorCode, String errorMessage) {
        return new JobResult(jobId, ResultType.FAILURE, null, errorCode, errorMessage);
    }

    public int getJobId() {
        return jobId;
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
        public JobResult deserialize(SerializeInput input, Frost frost) throws SerializationException {
            ResultType type = frost.readBoolean(input) ? ResultType.SUCCESS : ResultType.FAILURE;
            return switch (type) {
                case FAILURE -> {
                    int jobId = frost.readInt(input);
                    String errorMessage = frost.readString(input);
                    int errorCode = frost.readInt(input);
                    yield JobResult.failure(jobId, errorCode, errorMessage);
                }
                case SUCCESS -> JobResult.success(frost.readInt(input), frost.readBytes(input));
            };
        }

        @Override
        public void serialize(JobResult object, SerializeOutput output, Frost frost) throws SerializationException {
            frost.writeBoolean(object.getResultType() == ResultType.SUCCESS, output);
            switch (object.getResultType()) {
                case FAILURE -> {
                    frost.writeInt(object.getJobId(), output);
                    frost.writeString(object.getErrorMessage(), output);
                    frost.writeInt(object.getErrorCode(), output);
                }
                case SUCCESS -> {
                    frost.writeInt(object.getJobId(), output);
                    frost.writeBytes(object.getData(), output);
                }
            }
        }
    }
}