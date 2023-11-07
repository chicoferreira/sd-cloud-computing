package sd.cloudcomputing.client;

import sd.cloudcomputing.common.protocol.CSAuthPacket;
import sd.cloudcomputing.common.protocol.SCAuthResult;
import sd.cloudcomputing.common.serialization.Frost;

public class Main {

    public static void main(String[] args) {
        Frost frost = new Frost();
        frost.registerSerializer(CSAuthPacket.class, new CSAuthPacket.Serialization());
        frost.registerSerializer(SCAuthResult.class, new SCAuthResult.Serialization());
        new Client(frost).run();
    }

}
