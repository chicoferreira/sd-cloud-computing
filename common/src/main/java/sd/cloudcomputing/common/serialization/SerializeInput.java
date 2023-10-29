package sd.cloudcomputing.common.serialization;

import java.io.DataInputStream;
import java.io.IOException;

public class SerializeInput {

    private final DataInputStream stream;

    public SerializeInput(DataInputStream stream) {
        this.stream = stream;
    }

    public int readInt() throws IOException {
        return stream.readInt();
    }

    public long readLong() throws IOException {
        return stream.readLong();
    }

    public String readString() throws IOException {
        return stream.readUTF();
    }

    public byte[] readBytes() throws IOException {
        int length = stream.readInt();
        byte[] bytes = new byte[length];
        stream.readFully(bytes);
        return bytes;
    }

    public boolean readBoolean() throws IOException {
        return stream.readBoolean();
    }
}
