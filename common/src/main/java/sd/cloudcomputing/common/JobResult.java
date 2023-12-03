package sd.cloudcomputing.common;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

public sealed interface JobResult permits JobResult.Failure, JobResult.NoMemory, JobResult.Success {

    int PACKET_ID = 2;

    static JobResult success(int jobId, byte[] data) {
        return new Success(jobId, data);
    }

    static JobResult failure(int jobId, int errorCode, String errorMessage) {
        return new Failure(jobId, errorCode, errorMessage);
    }

    static JobResult noMemory(int jobId) {
        return new NoMemory(jobId);
    }

    default ResultType resultType() {
        return switch (this) {
            case Success ignored -> ResultType.SUCCESS;
            case Failure ignored -> ResultType.FAILURE;
            case NoMemory ignored -> ResultType.NO_MEMORY;
        };
    }

    int jobId();

    default JobResult mapId(int newId) {
        return switch (this) {
            case Success success -> JobResult.success(newId, success.data());
            case Failure failure -> JobResult.failure(newId, failure.errorCode(), failure.errorMessage());
            case NoMemory ignored -> JobResult.noMemory(newId);
        };
    }

    enum ResultType {
        SUCCESS,
        FAILURE,
        NO_MEMORY;

        public static ResultType fromOrdinal(int ordinal) {
            return switch (ordinal) {
                case 0 -> SUCCESS;
                case 1 -> FAILURE;
                case 2 -> NO_MEMORY;
                default -> throw new IllegalArgumentException("Unknown result type: " + ordinal);
            };
        }
    }

    record NoMemory(int jobId) implements JobResult {
    }

    record Success(int jobId, byte[] data) implements JobResult {
    }

    record Failure(int jobId, int errorCode, String errorMessage) implements JobResult {
    }

    class Serialization implements Serialize<JobResult> {

        @Override
        public @NotNull JobResult deserialize(SerializeInput input, Frost frost) throws IOException {
            ResultType resultType = ResultType.fromOrdinal(frost.readInt(input));
            return switch (resultType) {
                case SUCCESS -> JobResult.success(frost.readInt(input), frost.readBytes(input));
                case FAILURE -> JobResult.failure(frost.readInt(input), frost.readInt(input), frost.readString(input));
                case NO_MEMORY -> JobResult.noMemory(frost.readInt(input));
            };
        }

        @Override
        public void serialize(JobResult result, SerializeOutput output, Frost frost) throws IOException {
            frost.writeInt(result.resultType().ordinal(), output);
            switch (result) {
                case Failure failure -> {
                    frost.writeInt(failure.jobId(), output);
                    frost.writeInt(failure.errorCode(), output);
                    frost.writeString(failure.errorMessage(), output);
                }
                case Success success -> {
                    frost.writeInt(success.jobId(), output);
                    frost.writeBytes(success.data(), output);
                }
                case NoMemory noMemory -> frost.writeInt(noMemory.jobId(), output);
            }
        }
    }
}