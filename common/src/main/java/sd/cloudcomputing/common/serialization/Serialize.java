package sd.cloudcomputing.common.serialization;

public interface Serialize<T> {

    T deserialize(SerializeInput input, Frost frost) throws SerializationException;

    void serialize(T object, SerializeOutput output, Frost frost) throws SerializationException;

}
