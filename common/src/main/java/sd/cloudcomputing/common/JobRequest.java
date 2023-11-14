package sd.cloudcomputing.common;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

public record JobRequest(int jobId, byte[] data, int memoryNeeded) {

    public static final int PACKET_ID = 1;

    public static class Serialization implements Serialize<JobRequest> {

        @Override
        public @NotNull JobRequest deserialize(SerializeInput input, Frost frost) throws IOException {
            int jobId = frost.readInt(input);
            byte[] bytes = frost.readBytes(input);
            int memoryNeeded = frost.readInt(input);

            return new JobRequest(jobId, bytes, memoryNeeded);
        }

        @Override
        public void serialize(JobRequest object, SerializeOutput output, Frost frost) throws IOException {
            frost.writeInt(object.jobId(), output);
            frost.writeBytes(object.data(), output);
            frost.writeInt(object.memoryNeeded(), output);
        }
    }
}
