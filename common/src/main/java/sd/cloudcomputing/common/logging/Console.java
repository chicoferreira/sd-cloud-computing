package sd.cloudcomputing.common.logging;

import java.io.IOException;

public interface Console extends Logger {

    String readInput(String prompt);

    String readPassword(String prompt);

    void close() throws IOException;

}
