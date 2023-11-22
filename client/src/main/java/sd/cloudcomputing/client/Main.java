package sd.cloudcomputing.client;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.protocol.*;
import sd.cloudcomputing.common.serialization.Frost;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Frost frost = new Frost();
        frost.registerSerializer(CSAuthPacket.class, new CSAuthPacket.Serialization());
        frost.registerSerializer(SCAuthResult.class, new SCAuthResult.Serialization());
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());
        frost.registerSerializer(CSServerStatusRequestPacket.class, new CSServerStatusRequestPacket.Serialization());
        frost.registerSerializer(SCServerStatusResponsePacket.class, new SCServerStatusResponsePacket.Serialization());
        frost.registerSerializer(SCJobNotEnoughMemoryPacket.class, new SCJobNotEnoughMemoryPacket.Serialization());

        GenericPacketSerializer genericSerializer = new GenericPacketSerializer();
        genericSerializer.registerPacketId(JobRequest.PACKET_ID, JobRequest.class);
        genericSerializer.registerPacketId(JobResult.PACKET_ID, JobResult.class);
        genericSerializer.registerPacketId(SCJobNotEnoughMemoryPacket.PACKET_ID, SCJobNotEnoughMemoryPacket.class);
        genericSerializer.registerPacketId(SCServerStatusResponsePacket.PACKET_ID, SCServerStatusResponsePacket.class);
        genericSerializer.registerPacketId(CSServerStatusRequestPacket.PACKET_ID, CSServerStatusRequestPacket.class);
        frost.registerSerializer(GenericPacket.class, genericSerializer);

        new Application(frost).run();
    }

}
