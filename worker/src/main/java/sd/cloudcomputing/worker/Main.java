package sd.cloudcomputing.worker;

import sd23.JobFunction;
import sd23.JobFunctionException;

public class Main {
    public static void main(String[] args) {
        byte[] bytes = new byte[1024];
        new java.util.Random().nextBytes(bytes);

        try {
            JobFunction.execute(bytes);
        } catch (JobFunctionException e) {
            throw new RuntimeException(e);
        }
    }
}
