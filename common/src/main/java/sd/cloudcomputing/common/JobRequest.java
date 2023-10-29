package sd.cloudcomputing.common;

import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

public class JobRequest {

    private final int jobId;
    private final byte[] data;
    private final int memoryNeeded;

    public JobRequest(int jobId, byte[] data, int memoryNeeded) {
        this.jobId = jobId;
        this.data = data;
        this.memoryNeeded = memoryNeeded;
    }

    public int getJobId() {
        return jobId;
    }

    public byte[] getData() {
        return data;
    }

    public int getMemoryNeeded() {
        return memoryNeeded;
    }

    public static class Serialization implements Serialize<JobRequest> {

        @Override
        public JobRequest deserialize(SerializeInput input, Frost frost) {
            int jobId = frost.readInt(input);
            byte[] bytes = frost.readBytes(input);
            int memoryNeeded = frost.readInt(input);

            return new JobRequest(jobId, bytes, memoryNeeded);
        }

        @Override
        public void serialize(JobRequest object, SerializeOutput output, Frost frost) {
            frost.writeInt(object.getJobId(), output);
            frost.writeBytes(object.getData(), output);
            frost.writeInt(object.getMemoryNeeded(), output);
        }
    }
}
