package sd.cloudcomputing.common;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

public sealed interface WorkerJobResult permits WorkerJobResult.Failure, WorkerJobResult.Success {

    int jobId();

    JobResult toJobResult();

    record Success(int jobId, byte[] data) implements WorkerJobResult {
        @Override
        public JobResult toJobResult() {
            return new JobResult.Success(jobId, data);
        }
    }

    record Failure(int jobId, int errorCode, String errorMessage) implements WorkerJobResult {
        @Override
        public JobResult toJobResult() {
            return new JobResult.Failure(jobId, errorCode, errorMessage);
        }
    }

    class Serialization implements Serialize<WorkerJobResult> {

        @Override
        public @NotNull WorkerJobResult deserialize(SerializeInput input, Frost frost) throws IOException {
            boolean isSuccess = frost.readBoolean(input);
            if (isSuccess) {
                return new Success(frost.readInt(input), frost.readBytes(input));
            } else {
                return new Failure(frost.readInt(input), frost.readInt(input), frost.readString(input));
            }
        }

        @Override
        public void serialize(WorkerJobResult result, SerializeOutput output, Frost frost) throws IOException {
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
