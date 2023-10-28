package sd.cloudcomputing.common.serialization;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class SerializeOutput {

    private final DataOutputStream stream;

    public SerializeOutput(DataOutputStream stream) {
        this.stream = stream;
    }

    public void writeInt(int value) throws IOException {
        stream.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
        stream.writeLong(value);
    }

    public void writeString(@NotNull String value) throws IOException {
        stream.writeUTF(value);
    }

    public void writeBoolean(boolean value) throws IOException {
        stream.writeBoolean(value);
    }

    public void writeBytes(byte @NotNull [] value) throws IOException {
        stream.writeInt(value.length);
        stream.write(value);
    }

    public void flush() throws IOException {
        stream.flush();
    }
}
