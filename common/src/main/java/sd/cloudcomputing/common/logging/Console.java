package sd.cloudcomputing.common.logging;

import java.io.IOException;

public interface Console extends Logger {

    String readInput();

    void close() throws IOException;

}
