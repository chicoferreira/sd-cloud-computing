package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.Serialize;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.IOException;

public record SCServerStatusResponsePacket(int connectedWorkers, int totalCapacity, int maxPossibleMemory,
                                           int memoryUsagePercentage, int jobsCurrentlyRunning) {

    public static final int PACKET_ID = 4;

    public static final class Serialization implements Serialize<SCServerStatusResponsePacket> {

        @Override
        public @NotNull SCServerStatusResponsePacket deserialize(SerializeInput input, Frost frost) throws IOException {
            return new SCServerStatusResponsePacket(
                    frost.readInt(input),
                    frost.readInt(input),
                    frost.readInt(input),
                    frost.readInt(input),
                    frost.readInt(input)
            );
        }

        @Override
        public void serialize(SCServerStatusResponsePacket object, SerializeOutput output, Frost frost) throws IOException {
            frost.writeInt(object.connectedWorkers(), output);
            frost.writeInt(object.totalCapacity(), output);
            frost.writeInt(object.maxPossibleMemory(), output);
            frost.writeInt(object.memoryUsagePercentage(), output);
            frost.writeInt(object.jobsCurrentlyRunning(), output);
        }
    }

}
