package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.CSServerStatusRequestPacket;
import sd.cloudcomputing.common.protocol.GenericPacket;
import sd.cloudcomputing.common.protocol.SCJobNotEnoughMemoryPacket;
import sd.cloudcomputing.common.protocol.SCServerStatusResponsePacket;

public class ClientPacketHandler {

    private final Logger logger;
    private final ConnectedWorkerManager connectedWorkerManager;

    public ClientPacketHandler(Logger logger, ConnectedWorkerManager connectedWorkerManager) {
        this.logger = logger;
        this.connectedWorkerManager = connectedWorkerManager;
    }

    public void handlePacket(ClientConnection connection, GenericPacket packet) {
        Client client = connection.getClient();
        switch (packet.id()) {
            case JobRequest.PACKET_ID -> {
                JobRequest jobRequest = (JobRequest) packet.content();
                logger.info("Received job request with id " + jobRequest.jobId() + " and " + jobRequest.data().length + " bytes of data from " + client.getName());
                if (!connectedWorkerManager.scheduleJob(jobRequest)) {
                    logger.warn("No memory for " + jobRequest.jobId() + " from " + client.getName() + " with " + jobRequest.memoryNeeded() + " memory needed");
                    SCJobNotEnoughMemoryPacket notEnoughMemoryPacket = new SCJobNotEnoughMemoryPacket(jobRequest.jobId());
                    connection.enqueuePacket(new GenericPacket(SCJobNotEnoughMemoryPacket.PACKET_ID, notEnoughMemoryPacket));
                }
            }
            case CSServerStatusRequestPacket.PACKET_ID -> {
                logger.info("Received server status request from " + client.getName());

                SCServerStatusResponsePacket response = connectedWorkerManager.getServerStatus();

                connection.enqueuePacket(new GenericPacket(SCServerStatusResponsePacket.PACKET_ID, response));
            }
        }
    }

}
