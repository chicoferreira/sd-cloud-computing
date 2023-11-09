package sd.cloudcomputing.common;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sd.cloudcomputing.common.serialization.Frost;
import sd.cloudcomputing.common.serialization.SerializationException;
import sd.cloudcomputing.common.serialization.SerializeInput;
import sd.cloudcomputing.common.serialization.SerializeOutput;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobRequestSerializationTest {

    static final Frost frost = new Frost();

    @BeforeAll
    static void setup() {
        frost.registerSerializer(JobRequest.class, new JobRequest.Serialization());
    }

    @Test
    void testSerialization() throws SerializationException, IOException {
        JobRequest jobRequest = new JobRequest(0, new byte[]{1, 2, 3, 4, 5}, 500);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializeOutput output = new SerializeOutput(new DataOutputStream(out));

        frost.writeSerializable(jobRequest, JobRequest.class, output);
        frost.flush(output);

        byte[] serialized = out.toByteArray();

        ByteArrayInputStream in = new ByteArrayInputStream(serialized);
        SerializeInput input = new SerializeInput(new DataInputStream(in));

        JobRequest deserialized = frost.readSerializable(JobRequest.class, input);

        assertEquals(0, deserialized.getJobId());
        assertEquals(5, deserialized.getData().length);
        assertEquals(1, deserialized.getData()[0]);
        assertEquals(2, deserialized.getData()[1]);
        assertEquals(3, deserialized.getData()[2]);
        assertEquals(4, deserialized.getData()[3]);
        assertEquals(5, deserialized.getData()[4]);
        assertEquals(500, deserialized.getMemoryNeeded());
    }

}