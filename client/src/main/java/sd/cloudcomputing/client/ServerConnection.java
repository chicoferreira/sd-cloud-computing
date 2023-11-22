package sd.cloudcomputing.client;

import sd.cloudcomputing.common.AbstractConnection;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedInteger;
import sd.cloudcomputing.common.logging.Logger;
import sd.cloudcomputing.common.protocol.*;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Supplier;

public class ServerConnection extends AbstractConnection<GenericPacket, GenericPacket> {

    private final SynchronizedInteger currentJobId = new SynchronizedInteger(0);
    private final Application application;

    public ServerConnection(Logger logger, Frost frost, Application application, Socket socket) {
        super(GenericPacket.class, GenericPacket.class, logger, frost, socket);
        this.application = application;
    }

    public AuthenticateResult login(String username, String password) throws IOException, SerializationException {
        CSAuthPacket authPacket = new CSAuthPacket(username, password);

        SerializeOutput serializeOutput = super.writeEnd();

        super.getFrost().writeSerializable(authPacket, CSAuthPacket.class, serializeOutput);
        super.getFrost().flush(serializeOutput);

        SerializeInput serializeInput = super.readEnd();

        SCAuthResult scAuthResult = super.getFrost().readSerializable(SCAuthResult.class, serializeInput);

        return scAuthResult.result();
    }

    public int scheduleJob(byte[] bytes, int memory) {
        int jobId = currentJobId.getAndIncrement();

        JobRequest jobRequest = new JobRequest(jobId, bytes, memory);
        super.enqueuePacket(new GenericPacket(JobRequest.PACKET_ID, jobRequest)); // TODO: refactor this api

        return jobId;
    }

    public void sendServerStatusRequest() throws IOException, SerializationException {
        SerializeOutput serializeOutput = super.writeEnd();

        GenericPacket packet = new GenericPacket(CSServerStatusRequestPacket.PACKET_ID, new CSServerStatusRequestPacket());
        super.getFrost().writeSerializable(packet, GenericPacket.class, serializeOutput);
        super.getFrost().flush(serializeOutput);
    }

    @Override
    public void handlePacket(GenericPacket packet) {
        switch (packet.id()) {
            case SCJobNotEnoughMemoryPacket.PACKET_ID:
                SCJobNotEnoughMemoryPacket notEnoughMemoryPacket = (SCJobNotEnoughMemoryPacket) packet.content();
                getLogger().info("Job " + notEnoughMemoryPacket.jobId() + " failed due to not enough memory");
                break;
            case SCServerStatusResponsePacket.PACKET_ID:
                SCServerStatusResponsePacket responsePacket = (SCServerStatusResponsePacket) packet.content();
                getLogger().info("Server status (" + this.getSocket().getAddressWithPort() + "): ");
                getLogger().info(" - Connected workers: " + responsePacket.connectedWorkers());
                getLogger().info(" - Total capacity: " + responsePacket.totalCapacity() + "MB");
                getLogger().info(" - Max possible memory: " + responsePacket.maxPossibleMemory() + "MB");
                getLogger().info(" - Memory usage: " + responsePacket.memoryUsagePercentage() + "%");
                getLogger().info(" - Jobs running: " + responsePacket.jobsCurrentlyRunning());
                break;
            case JobResult.PACKET_ID:
                JobResult jobResult = (JobResult) packet.content();
                application.notifyJobResult(jobResult);
                break;
            default:
                getLogger().warn("Unknown packet (id=" + packet.id() + "): " + packet.content());
                break;
        }
    }

    @Override
    protected void onDisconnect() {
        super.getLogger().info("Disconnected from server");
        application.notifyServerDisconnect();
    }

    public boolean start(Supplier<String> usernameSupplier, Supplier<String> passwordSupplier) {
        try {
            String username = usernameSupplier.get();
            String password = passwordSupplier.get();

            AuthenticateResult result = this.login(username, password);
            if (result.isSuccess()) {
                getLogger().info("Successfully authenticated as " + username + " (" + result + ")");
                this.startReadWrite();
                return true;
            } else {
                getLogger().info("Failed to authenticate: " + result);
                this.disconnect();
            }
        } catch (IOException e) {
            getLogger().error("Error sending packet to server: " + e.getMessage());
        } catch (SerializationException e) {
            getLogger().error("Error serializing auth packet: " + e.getMessage());
        }
        return false;
    }
}
