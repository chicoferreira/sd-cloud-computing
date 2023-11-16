package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;

import java.io.IOException;

/**
 * Packet sent from the server to the client when there is not any worker with enough memory to run the job.
 *
 * @param jobId of the job that could not be run
 */
public record SCJobNotEnoughMemoryPacket(int jobId) {

    public static final int PACKET_ID = 5;

    public static class Serialization implements Serialize<SCJobNotEnoughMemoryPacket> {
        @Override
        public @NotNull SCJobNotEnoughMemoryPacket deserialize(SerializeInput input, Frost frost) throws SerializationException, IOException {
            return new SCJobNotEnoughMemoryPacket(frost.readInt(input));
        }

        @Override
        public void serialize(SCJobNotEnoughMemoryPacket object, SerializeOutput output, Frost frost) throws SerializationException, IOException {
            frost.writeInt(object.jobId(), output);
        }
    }
}
