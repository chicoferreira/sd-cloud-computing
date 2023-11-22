package sd.cloudcomputing.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.concurrent.SynchronizedInteger;
import sd.cloudcomputing.common.concurrent.SynchronizedMap;

/**
 * Manages the mapping of client job requests to server job requests and vice versa.
 * <p>
 * This is necessary because each client has its own current job ID counter.
 * To not expose client information to the workers we need to map the client job ID to a new job ID.
 */
public class JobMappingService {

    private final SynchronizedMap<Integer, Integer> serverJobIdToClientJobIdMap = new SynchronizedMap<>();
    private final SynchronizedMap<Integer, Client> serverJobIdToClient = new SynchronizedMap<>();

    private final SynchronizedInteger nextServerJobId = new SynchronizedInteger(0);

    /**
     * Maps a client job request to be ready to be sent to a worker.
     * This also saves the mapping for the reverse operation when the result is received.
     *
     * @param client           The client that requested the job
     * @param clientJobRequest The client job request
     * @return The same job request with a new job ID
     */
    public JobRequest mapClientRequestToServerRequest(Client client, @NotNull JobRequest clientJobRequest) {
        int serverJobId = nextServerJobId.incrementAndGet();

        serverJobIdToClientJobIdMap.put(serverJobId, clientJobRequest.jobId());
        serverJobIdToClient.put(serverJobId, client);

        return new JobRequest(serverJobId, clientJobRequest.data(), clientJobRequest.memoryNeeded());
    }

    /**
     * Maps a server job result to the client job result to be sent to the client.
     * This is a one time operation as it removes the mapping when the result is received.
     * <p>
     * ({@link JobMappingService#retrieveClientFromServerJobId(int)} does not affect this mapping)
     *
     * @param jobResult The job result received from the worker
     * @return The same job result with the client job ID or null if the mapping was not found
     */
    public @Nullable JobResult retrieveClientResultFromServerResult(JobResult jobResult) {
        Integer clientJobId = serverJobIdToClientJobIdMap.remove(jobResult.jobId());
        if (clientJobId == null) {
            return null;
        }

        return switch (jobResult) {
            case JobResult.Success success -> JobResult.success(clientJobId, success.data());
            case JobResult.Failure failure ->
                    JobResult.failure(clientJobId, failure.errorCode(), failure.errorMessage());
        };
    }

    /**
     * Maps a server job ID to the client that requested the job.
     * This is a one time operation as it removes the mapping when the result is received.
     * <p>
     * ({@link JobMappingService#retrieveClientResultFromServerResult(JobResult)} does not affect this mapping)
     *
     * @param serverJobId The server job ID
     * @return The client that requested the job or null if the mapping was not found
     */
    public @Nullable Client retrieveClientFromServerJobId(int serverJobId) {
        return serverJobIdToClient.remove(serverJobId);
    }

    /**
     * Deletes the client and its job ID mapping for the given server job ID.
     * Used when the job request could not be scheduled (e.g., not enough memory).
     *
     * @param serverJobId The server job ID to delete the mapping for
     */
    public void deleteMapping(int serverJobId) {
        serverJobIdToClientJobIdMap.remove(serverJobId);
        serverJobIdToClient.remove(serverJobId);
    }
}
