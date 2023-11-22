package sd.cloudcomputing.common;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

public sealed interface JobResult permits JobResult.Success, JobResult.Failure {

    int PACKET_ID = 2;

    static JobResult success(int jobId, byte[] data) {
        return new Success(jobId, data);
    }

    static JobResult failure(int jobId, int errorCode, String errorMessage) {
        return new Failure(jobId, errorCode, errorMessage);
    }

    default ResultType resultType() {
        return switch (this) {
            case Success ignored -> ResultType.SUCCESS;
            case Failure ignored -> ResultType.FAILURE;
        };
    }

    int jobId();

    enum ResultType {
        SUCCESS,
        FAILURE
    }

    record Success(int jobId, byte[] data) implements JobResult {
    }

    record Failure(int jobId, int errorCode, String errorMessage) implements JobResult {
    }

    class Serialization implements Serialize<JobResult> {

        @Override
        public @NotNull JobResult deserialize(SerializeInput input, Frost frost) throws IOException {
            if (frost.readBoolean(input)) {
                return JobResult.success(frost.readInt(input), frost.readBytes(input));
            } else {
                return JobResult.failure(frost.readInt(input), frost.readInt(input), frost.readString(input));
            }
        }

        @Override
        public void serialize(JobResult result, SerializeOutput output, Frost frost) throws IOException {
            frost.writeBoolean(result instanceof Success, output);
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
            }
        }
    }
}