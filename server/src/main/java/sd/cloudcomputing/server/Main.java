package sd.cloudcomputing.server;

import sd.cloudcomputing.common.JobRequest;
import sd.cloudcomputing.common.JobResult;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.*;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws IOException, SerializationException {
        Socket workerSocket = new Socket("localhost", 9900);
        OutputStream outputStream = workerSocket.getOutputStream();

        Frost frost = new Frost();
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());

        SerializeOutput serializeOutput = new SerializeOutput(new DataOutputStream(outputStream));

        for (int i = 0; i < 100; i++) {
            JobRequest jobRequest = new JobRequest(i, "Hello World!".getBytes(), 100);
            frost.writeSerializable(jobRequest, JobRequest.class, serializeOutput);
            frost.flush(serializeOutput);
            try {
                Thread.sleep(10); // TODO: unsure why this is needed
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        InputStream inputStream = workerSocket.getInputStream();
        SerializeInput serializeInput = new SerializeInput(new DataInputStream(new BufferedInputStream(inputStream)));

        JobResult receivedJobResult = frost.readSerializable(JobResult.class, serializeInput);
        System.out.println(receivedJobResult.getJobId());
        System.out.println(new String(receivedJobResult.getData()));
        System.out.println(receivedJobResult.getErrorCode());
        System.out.println(receivedJobResult.getErrorMessage());
    }

}
