package sd.cloudcomputing.common;

import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

public class JobRequest {

    private final byte[] data;

    public JobRequest(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public static class Serialization implements Serialize<JobRequest> {

        @Override
        public JobRequest deserialize(SerializeInput input, Frost frost) {
            byte[] bytes = frost.readBytes(input);

            return new JobRequest(bytes);
        }

        @Override
        public void serialize(JobRequest object, SerializeOutput output, Frost frost) {
            frost.writeBytes(object.getData(), output);
        }
    }
}
