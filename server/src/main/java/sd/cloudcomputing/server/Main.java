package sd.cloudcomputing.server;

import sd.cloudcomputing.common.serialization.Frost;

public class Main {

    public static void main(String[] args) {
        Frost frost = new Frost();
        Server server = new Server(frost);
        server.run(8080, 9900);
    }
}
