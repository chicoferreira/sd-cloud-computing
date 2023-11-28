package sd.cloudcomputing.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.concurrent.SynchronizedInteger;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;

/**
 * Manages the mapping of client job requests to server job requests and vice versa.
 * <p>
 * This is necessary because each client has its own current job ID counter.
 * <p>
 * To not expose client information to the workers we need to map the client job ID
 * to a new job ID that is unique and managed by the server.
 */
public class JobMappingService {

    private final SynchronizedMap<Integer, Mapping> serverJobIdToMapping = new SynchronizedMap<>();

    private final SynchronizedInteger nextServerJobId = new SynchronizedInteger(0);

    public JobRequest mapClientRequestToServerRequest(Client client, @NotNull JobRequest clientJobRequest) {
        int serverJobId = nextServerJobId.getAndIncrement();

        serverJobIdToMapping.put(serverJobId, new Mapping(serverJobId, clientJobRequest.jobId(), client));

        return new JobRequest(serverJobId, clientJobRequest.data(), clientJobRequest.memoryNeeded());
    }

    public @Nullable Mapping retrieveMappingFromServerJobId(int serverJobId) {
        return serverJobIdToMapping.remove(serverJobId);
    }

    public @Nullable Mapping getMappingFromServerJobId(int serverJobId) {
        return serverJobIdToMapping.get(serverJobId);
    }

    public void deleteMapping(int serverJobId) {
        serverJobIdToMapping.remove(serverJobId);
    }

    public record Mapping(int serverJobId, int clientJobId, Client client) {
    }
}
