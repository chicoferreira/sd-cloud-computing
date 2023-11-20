package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.CSServerStatusRequestPacket;
import sd.cloudcomputing.common.protocol.GenericPacket;
import sd.cloudcomputing.common.protocol.SCServerStatusResponsePacket;

public class ClientPacketHandler {

    private final Logger logger;
    private final ConnectedWorkerManager connectedWorkerManager;
    private final Server server;

    public ClientPacketHandler(Logger logger, ConnectedWorkerManager connectedWorkerManager, Server server) {
        this.logger = logger;
        this.connectedWorkerManager = connectedWorkerManager;
        this.server = server;
    }

    public void handlePacket(ClientConnection connection, GenericPacket packet) {
        Client client = connection.getClient();
        switch (packet.id()) {
            case JobRequest.PACKET_ID -> {
                JobRequest jobRequest = (JobRequest) packet.content();
                server.queueClientJobRequest(client, connection, jobRequest);
            }
            case CSServerStatusRequestPacket.PACKET_ID -> {
                logger.info("Received server status request from " + client.getName());

                SCServerStatusResponsePacket response = connectedWorkerManager.getServerStatus();

                connection.enqueuePacket(new GenericPacket(SCServerStatusResponsePacket.PACKET_ID, response));
            }
        }
    }

}
