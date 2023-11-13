package sd.cloudcomputing.client;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.concurrent.BoundedBuffer;
import sd.cloudcomputing.common.protocol.GenericPacket;

import java.util.concurrent.locks.ReentrantLock;

public class ClientPacketDispatcher extends BoundedBuffer<GenericPacket> {

    private final ReentrantLock currentJobIdLock = new ReentrantLock();
    private int currentJobId = 0;

    public ClientPacketDispatcher() {
        super(100);
    }

    /**
     * @param bytes  The bytes of the job to run
     * @param memory The amount of memory needed to run the job
     * @return The id of the job
     * @throws InterruptedException If the thread is interrupted while waiting for the buffer to have space
     */
    public int scheduleJob(byte[] bytes, int memory) throws InterruptedException {
        currentJobIdLock.lock();
        int jobId = currentJobId++;
        currentJobIdLock.unlock();

        JobRequest jobRequest = new JobRequest(jobId, bytes, memory);
        super.put(new GenericPacket(JobRequest.PACKET_ID, jobRequest)); // TODO: refactor this api

        return jobId;
    }

}
