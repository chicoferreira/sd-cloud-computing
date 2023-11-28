package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.WorkerJobResult;
import sd.cloudcomputing.common.protocol.*;
import sd.cloudcomputing.common.serialization.Frost;

public class Main {

    public static void main(String[] args) {
        Frost frost = new Frost();
        frost.registerSerializer(WSHandshakePacket.class, new WSHandshakePacket.Serialization());
        frost.registerSerializer(CSAuthPacket.class, new CSAuthPacket.Serialization());
        frost.registerSerializer(SCAuthResult.class, new SCAuthResult.Serialization());
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());
        frost.registerSerializer(CSServerStatusRequestPacket.class, new CSServerStatusRequestPacket.Serialization());
        frost.registerSerializer(SCServerStatusResponsePacket.class, new SCServerStatusResponsePacket.Serialization());
        frost.registerSerializer(WorkerJobResult.class, new WorkerJobResult.Serialization());

        GenericPacketSerializer serializer = new GenericPacketSerializer();
        serializer.registerPacketId(JobRequest.PACKET_ID, JobRequest.class);
        serializer.registerPacketId(JobResult.PACKET_ID, JobResult.class);
        serializer.registerPacketId(CSServerStatusRequestPacket.PACKET_ID, CSServerStatusRequestPacket.class);
        serializer.registerPacketId(SCServerStatusResponsePacket.PACKET_ID, SCServerStatusResponsePacket.class);

        frost.registerSerializer(GenericPacket.class, serializer);
        Server server = new Server(frost);
        server.run(8080, 9900);
    }
}
