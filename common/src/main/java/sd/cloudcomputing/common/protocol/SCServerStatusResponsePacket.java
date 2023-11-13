package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;

import java.io.IOException;

public record SCServerStatusResponsePacket(int connectedWorkers, int totalMemoryCombined, int maxMemory,
                                           int memoryUsagePercentage, int jobsCurrentlyRunning) {

    public static final int PACKET_ID = 4;

    public static final class Serialization implements Serialize<SCServerStatusResponsePacket> {

        @Override
        public @NotNull SCServerStatusResponsePacket deserialize(SerializeInput input, Frost frost) throws SerializationException, IOException {
            return new SCServerStatusResponsePacket(
                    frost.readInt(input),
                    frost.readInt(input),
                    frost.readInt(input),
                    frost.readInt(input),
                    frost.readInt(input)
            );
        }

        @Override
        public void serialize(SCServerStatusResponsePacket object, SerializeOutput output, Frost frost) throws SerializationException, IOException {
            frost.writeInt(object.connectedWorkers(), output);
            frost.writeInt(object.totalMemoryCombined(), output);
            frost.writeInt(object.maxMemory(), output);
            frost.writeInt(object.memoryUsagePercentage(), output);
            frost.writeInt(object.jobsCurrentlyRunning(), output);
        }
    }

}
