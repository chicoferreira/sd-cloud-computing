package sd.cloudcomputing.common.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Frost {

    private final Map<Class<?>, Serialize<?>> customSerializers = new HashMap<>();

    public void writeInt(int value, SerializeOutput output) {
        try {
            output.writeInt(value);
        } catch (IOException e) {
            throw new SerializationException("writeInt", e);
        }
    }

    public void writeLong(long value, SerializeOutput output) {
        try {
            output.writeLong(value);
        } catch (IOException e) {
            throw new SerializationException("writeLong", e);
        }
    }

    public void writeString(@NotNull String value, SerializeOutput output) {
        try {
            output.writeString(value);
        } catch (IOException e) {
            throw new SerializationException("writeString", e);
        }
    }

    public void writeBoolean(boolean value, SerializeOutput output) {
        try {
            output.writeBoolean(value);
        } catch (IOException e) {
            throw new SerializationException("writeBoolean", e);
        }
    }

    public <T> void writeSerializable(@NotNull T value, Class<T> clazz, SerializeOutput output) {
        Serialize<T> serializer = getSerializer(clazz);
        if (serializer == null) {
            throw new IllegalArgumentException("No serializer has been registered for class " + clazz.getName());
        }
        serializer.serialize(value, output, this);
    }

    public void writeBytes(byte @NotNull [] value, SerializeOutput output) {
        try {
            output.writeBytes(value);
        } catch (IOException e) {
            throw new SerializationException("writeBytes", e);
        }
    }

    public void flush(SerializeOutput output) {
        try {
            output.flush();
        } catch (IOException e) {
            throw new SerializationException("flush", e);
        }
    }

    public int readInt(SerializeInput input) {
        try {
            return input.readInt();
        } catch (IOException e) {
            throw new SerializationException("readInt", e);
        }
    }

    public long readLong(SerializeInput input) {
        try {
            return input.readLong();
        } catch (IOException e) {
            throw new SerializationException("readLong", e);
        }
    }

    public String readString(SerializeInput input) {
        try {
            return input.readString();
        } catch (IOException e) {
            throw new SerializationException("readString", e);
        }
    }

    public boolean readBoolean(SerializeInput input) {
        try {
            return input.readBoolean();
        } catch (IOException e) {
            throw new SerializationException("readBoolean", e);
        }
    }

    public <T> T readSerializable(Class<T> clazz, SerializeInput input) {
        Serialize<T> serializer = getSerializer(clazz);
        if (serializer == null) {
            throw new IllegalArgumentException("No serializer has been registered for class " + clazz.getName());
        }
        return serializer.deserialize(input, this);
    }

    public byte @NotNull [] readBytes(SerializeInput input) {
        try {
            return input.readBytes();
        } catch (IOException e) {
            throw new SerializationException("readBytes", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> @Nullable Serialize<T> getSerializer(Class<T> clazz) {
        return (Serialize<T>) customSerializers.get(clazz);
    }

    public <T> void registerSerializer(Class<T> clazz, Serialize<T> serializer) {
        customSerializers.put(clazz, serializer);
    }

}
