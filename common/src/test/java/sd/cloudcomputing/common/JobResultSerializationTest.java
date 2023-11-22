package sd.cloudcomputing.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

        assertInstanceOf(JobResult.Success.class, deserialized);

        JobResult.Success success = (JobResult.Success) deserialized;

        assertEquals(1, success.jobId());
        assertEquals(5, success.data().length);
        assertEquals(1, success.data()[0]);
        assertEquals(2, success.data()[1]);
        assertEquals(3, success.data()[2]);
        assertEquals(4, success.data()[3]);
        assertEquals(5, success.data()[4]);
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

        assertInstanceOf(JobResult.Failure.class, deserialized);

        JobResult.Failure failure = (JobResult.Failure) deserialized;

        assertEquals(1, failure.jobId());
        assertEquals(-1, failure.errorCode());
        assertEquals("error", failure.errorMessage());
    }
}