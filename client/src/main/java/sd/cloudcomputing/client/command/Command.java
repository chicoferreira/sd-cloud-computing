package sd.cloudcomputing.client.command;

import sd.cloudcomputing.common.logging.Logger;

public interface Command {

    String getName();

    String getUsage();

    void execute(Logger logger, String[] args);

}
