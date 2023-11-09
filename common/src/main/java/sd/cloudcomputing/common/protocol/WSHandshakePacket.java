package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

/**
 * Worker to Server handshake packet
 */
public record WSHandshakePacket(int maxMemoryCapacity) {

    public static class Serialization implements Serialize<WSHandshakePacket> {

        @Override
        public @NotNull WSHandshakePacket deserialize(SerializeInput input, Frost frost) throws IOException {
            return new WSHandshakePacket(frost.readInt(input));
        }

        @Override
        public void serialize(WSHandshakePacket object, SerializeOutput output, Frost frost) throws IOException {
            frost.writeInt(object.maxMemoryCapacity(), output);
        }
    }
}
