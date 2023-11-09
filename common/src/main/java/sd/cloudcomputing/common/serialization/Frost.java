package sd.cloudcomputing.common.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Frost {

    private final Map<Class<?>, Serialize<?>> customSerializers = new HashMap<>();

    public void writeInt(int value, SerializeOutput output) throws IOException {
        output.writeInt(value);
    }

    public void writeLong(long value, SerializeOutput output) throws IOException {
        output.writeLong(value);
    }

    public void writeString(@NotNull String value, SerializeOutput output) throws IOException {
        output.writeString(value);
    }

    public void writeBoolean(boolean value, SerializeOutput output) throws IOException {
        output.writeBoolean(value);
    }

    public <T> void writeSerializable(@NotNull T value, Class<T> clazz, SerializeOutput output) throws SerializationException, IOException {
        Serialize<T> serializer = getSerializer(clazz);
        if (serializer == null) {
            throw new SerializationException("No serializer has been registered for class " + clazz.getName());
        }
        serializer.serialize(value, output, this);
    }

    public void writeBytes(byte @NotNull [] value, SerializeOutput output) throws IOException {
        output.writeBytes(value);
    }

    public void flush(SerializeOutput output) throws IOException {
        output.flush();
    }

    public int readInt(SerializeInput input) throws IOException {
        return input.readInt();
    }

    public long readLong(SerializeInput input) throws IOException {
        return input.readLong();
    }

    public String readString(SerializeInput input) throws IOException {
        return input.readString();
    }

    public boolean readBoolean(SerializeInput input) throws IOException {
        return input.readBoolean();
    }

    public <T> @NotNull T readSerializable(Class<T> clazz, SerializeInput input) throws SerializationException, IOException {
        Serialize<T> serializer = getSerializer(clazz);
        if (serializer == null) {
            throw new SerializationException("No serializer has been registered for class " + clazz.getName());
        }
        return serializer.deserialize(input, this);
    }

    public byte @NotNull [] readBytes(SerializeInput input) throws IOException {
        return input.readBytes();
    }

    @SuppressWarnings("unchecked")
    private <T> @Nullable Serialize<T> getSerializer(Class<T> clazz) {
        return (Serialize<T>) customSerializers.get(clazz);
    }

    public <T> void registerSerializer(Class<T> clazz, @NotNull Serialize<T> serializer) {
        customSerializers.put(clazz, serializer);
    }

}
