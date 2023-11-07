package sd.cloudcomputing.server;

import sd.cloudcomputing.common.protocol.WSHandshakePacket;
import sd.cloudcomputing.common.serialization.Frost;

public class Main {

    public static void main(String[] args) {
        Frost frost = new Frost();
        frost.registerSerializer(WSHandshakePacket.class, new WSHandshakePacket.Serialization());
        Server server = new Server(frost);
        server.run(8080, 9900);
    }
}
