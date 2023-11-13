package sd.cloudcomputing.client;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.protocol.CSAuthPacket;
import sd.cloudcomputing.common.protocol.GenericPacket;
import sd.cloudcomputing.common.protocol.GenericPacketSerializer;
import sd.cloudcomputing.common.protocol.SCAuthResult;
import sd.cloudcomputing.common.serialization.Frost;

public class Main {

    public static void main(String[] args) {
        Frost frost = new Frost();
        frost.registerSerializer(CSAuthPacket.class, new CSAuthPacket.Serialization());
        frost.registerSerializer(SCAuthResult.class, new SCAuthResult.Serialization());

        GenericPacketSerializer genericSerializer = new GenericPacketSerializer();
        genericSerializer.registerPacketId(JobRequest.PACKET_ID, JobRequest.class);
        frost.registerSerializer(GenericPacket.class, genericSerializer);

        new Application(frost).run();
    }

}
