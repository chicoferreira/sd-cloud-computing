package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

public record CSAuthPacket(String username, String password) {

    public static class Serialization implements Serialize<CSAuthPacket> {

        @Override
        public @NotNull CSAuthPacket deserialize(SerializeInput input, Frost frost) throws IOException {
            String username = frost.readString(input);
            String password = frost.readString(input);
            return new CSAuthPacket(username, password);
        }

        @Override
        public void serialize(CSAuthPacket object, SerializeOutput output, Frost frost) throws IOException {
            frost.writeString(object.username(), output);
            frost.writeString(object.password(), output);
        }
    }

}