package sd.cloudcomputing.client.api;

import sd.cloudcomputing.common.protocol.SCServerStatusResponsePacket;

public interface ServerSessionListener {

    void onServerStatusResponse(ServerSession serverSession, SCServerStatusResponsePacket serverStatusResponse);

    void onJobResultResponse(ServerSession serverSession, ClientJob.Received clientJob);

    void onDisconnect(ServerSession serverSession);

}
