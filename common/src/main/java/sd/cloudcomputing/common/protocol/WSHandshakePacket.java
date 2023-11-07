package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;

/**
 * Worker to Server handshake packet
 */
public record WSHandshakePacket(int maxMemoryCapacity) {

    public static class Serialization implements Serialize<WSHandshakePacket> {

        @Override
        public @NotNull WSHandshakePacket deserialize(SerializeInput input, Frost frost) throws SerializationException {
            return new WSHandshakePacket(frost.readInt(input));
        }

        @Override
        public void serialize(WSHandshakePacket object, SerializeOutput output, Frost frost) throws SerializationException {
            frost.writeInt(object.maxMemoryCapacity(), output);
        }
    }
}
