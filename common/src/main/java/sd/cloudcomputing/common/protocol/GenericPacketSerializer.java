package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A packet that has an id prefix used to identify which packet it is
 * Used when packets are sent that have unexpected order
 * <p>
 * Examples of GenericPacket: Job result sent from the server to the client, Server status sent from the server to the client
 * Either of these packets can be sent at any time, so they need to have an id prefix to identify them
 * <p>
 * Examples of not GenericPacket: Handshakes (they are sent when the connection is first established and so are expected)
 */
public class GenericPacketSerializer implements Serialize<GenericPacket> { // there's maybe a better name for this

    private final Map<Integer, Class<?>> packetIdToClass;

    public GenericPacketSerializer() {
        this.packetIdToClass = new HashMap<>();
    }

    public void registerPacketId(int id, Class<?> serializer) {
        packetIdToClass.put(id, serializer);
    }

    @Override
    public @NotNull GenericPacket deserialize(SerializeInput input, Frost frost) throws SerializationException, IOException {
        int packetId = frost.readInt(input);

        Class<?> serializer = packetIdToClass.get(packetId);
        if (serializer == null) {
            throw new SerializationException("Unknown packet id: " + packetId);
        }

        return new GenericPacket(packetId, frost.readSerializable(serializer, input));
    }

    @Override
    public void serialize(GenericPacket packet, SerializeOutput output, Frost frost) throws SerializationException, IOException {
        @SuppressWarnings("unchecked") Class<Object> clazz = (Class<Object>) packetIdToClass.get(packet.id());

        if (clazz == null) {
            throw new SerializationException("Unknown packet id: " + packet.id());
        }

        frost.writeInt(packet.id(), output);
        frost.writeSerializable(packet.content(), clazz, output);
    }
}
