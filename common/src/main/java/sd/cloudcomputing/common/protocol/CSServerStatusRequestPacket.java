package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;

import java.io.IOException;

public record CSServerStatusRequestPacket() {
    public static final int PACKET_ID = 3;

    public static final class Serialization implements Serialize<CSServerStatusRequestPacket> {
        @Override
        public @NotNull CSServerStatusRequestPacket deserialize(SerializeInput input, Frost frost) throws SerializationException, IOException {
            return new CSServerStatusRequestPacket();
        }

        @Override
        public void serialize(CSServerStatusRequestPacket object, SerializeOutput output, Frost frost) throws SerializationException, IOException {
        }
    }

}
