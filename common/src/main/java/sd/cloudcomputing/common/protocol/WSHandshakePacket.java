package sd.cloudcomputing.common.protocol;

import sd.cloudcomputing.common.serialization.*;

/**
 * Worker to Server handshake packet
 */
public class WSHandshakePacket {

    private final int maxMemoryCapacity;

    public WSHandshakePacket(int maxMemoryCapacity) {
        this.maxMemoryCapacity = maxMemoryCapacity;
    }

    public int getMaxMemoryCapacity() {
        return maxMemoryCapacity;
    }

    public static class Serialization implements Serialize<WSHandshakePacket> {

        @Override
        public WSHandshakePacket deserialize(SerializeInput input, Frost frost) throws SerializationException {
            return new WSHandshakePacket(frost.readInt(input));
        }

        @Override
        public void serialize(WSHandshakePacket object, SerializeOutput output, Frost frost) throws SerializationException {
            frost.writeInt(object.getMaxMemoryCapacity(), output);
        }
    }
}
