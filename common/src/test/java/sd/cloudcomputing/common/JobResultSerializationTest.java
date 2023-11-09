package sd.cloudcomputing.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobResultSerializationTest {

    static final Frost frost = new Frost();

    @BeforeAll
    static void setup() {
        frost.registerSerializer(JobResult.class, new JobResult.Serialization());
    }

    @Test
    void testSerializationResultSuccess() throws SerializationException, IOException {
        JobResult jobResult = JobResult.success(1, new byte[]{1, 2, 3, 4, 5});

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializeOutput output = new SerializeOutput(new DataOutputStream(out));

        frost.writeSerializable(jobResult, JobResult.class, output);
        frost.flush(output);

        byte[] serialized = out.toByteArray();

        ByteArrayInputStream in = new ByteArrayInputStream(serialized);
        SerializeInput input = new SerializeInput(new DataInputStream(in));

        JobResult deserialized = frost.readSerializable(JobResult.class, input);

        assertEquals(1, deserialized.getJobId());
        assertEquals(JobResult.ResultType.SUCCESS, deserialized.getResultType());
        assertEquals(5, deserialized.getData().length);
        assertEquals(1, deserialized.getData()[0]);
        assertEquals(2, deserialized.getData()[1]);
        assertEquals(3, deserialized.getData()[2]);
        assertEquals(4, deserialized.getData()[3]);
        assertEquals(5, deserialized.getData()[4]);
    }

    @Test
    void testSerializationResultFailure() throws SerializationException, IOException {
        JobResult jobResult = JobResult.failure(1, -1, "error");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializeOutput output = new SerializeOutput(new DataOutputStream(out));

        frost.writeSerializable(jobResult, JobResult.class, output);
        frost.flush(output);

        byte[] serialized = out.toByteArray();

        ByteArrayInputStream in = new ByteArrayInputStream(serialized);
        SerializeInput input = new SerializeInput(new DataInputStream(in));

        JobResult deserialized = frost.readSerializable(JobResult.class, input);

        assertEquals(1, deserialized.getJobId());
        assertEquals(JobResult.ResultType.FAILURE, deserialized.getResultType());
        assertEquals(-1, deserialized.getErrorCode());
        assertEquals("error", deserialized.getErrorMessage());
    }
}