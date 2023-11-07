package sd.cloudcomputing.common.serialization;

import org.jetbrains.annotations.NotNull;

public interface Serialize<T> {

    @NotNull T deserialize(SerializeInput input, Frost frost) throws SerializationException;

    void serialize(T object, SerializeOutput output, Frost frost) throws SerializationException;

}
