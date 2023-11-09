package sd.cloudcomputing.common.serialization;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface Serialize<T> {

    @NotNull T deserialize(SerializeInput input, Frost frost) throws SerializationException, IOException;

    void serialize(T object, SerializeOutput output, Frost frost) throws SerializationException, IOException;

}
