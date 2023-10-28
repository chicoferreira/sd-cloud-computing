package sd.cloudcomputing.common.serialization;

public interface Serialize<T> {

    T deserialize(SerializeInput input, Frost frost);

    void serialize(T object, SerializeOutput output, Frost frost);

}
