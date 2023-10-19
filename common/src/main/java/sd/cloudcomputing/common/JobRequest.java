package sd.cloudcomputing.common;

public class JobRequest {

    private final byte[] data;

    public JobRequest(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
