package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

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
        public @NotNull CSAuthPacket deserialize(SerializeInput input, Frost frost) throws IOException {
            String username = frost.readString(input);
            String password = frost.readString(input);
            return new CSAuthPacket(username, password);
        }

        @Override
        public void serialize(CSAuthPacket object, SerializeOutput output, Frost frost) throws IOException {
            frost.writeString(object.getUsername(), output);
            frost.writeString(object.getPassword(), output);
        }
    }

}