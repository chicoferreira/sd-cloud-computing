package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

public record CSServerStatusRequestPacket() {
    public static final int PACKET_ID = 3;

    public static final class Serialization implements Serialize<CSServerStatusRequestPacket> {
        @Override
        public @NotNull CSServerStatusRequestPacket deserialize(SerializeInput input, Frost frost) {
            return new CSServerStatusRequestPacket();
        }

        @Override
        public void serialize(CSServerStatusRequestPacket object, SerializeOutput output, Frost frost) {
        }
    }

}
