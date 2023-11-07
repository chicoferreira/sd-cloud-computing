package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;

public class CSAuthPacket {

    private final String username;
    private final String password;

    public CSAuthPacket(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public static class Serialization implements Serialize<CSAuthPacket> {

        @Override
        public @NotNull CSAuthPacket deserialize(SerializeInput input, Frost frost) throws SerializationException {
            String username = frost.readString(input);
            String password = frost.readString(input);
            return new CSAuthPacket(username, password);
        }

        @Override
        public void serialize(CSAuthPacket object, SerializeOutput output, Frost frost) throws SerializationException {
            frost.writeString(object.getUsername(), output);
            frost.writeString(object.getPassword(), output);
        }
    }

}