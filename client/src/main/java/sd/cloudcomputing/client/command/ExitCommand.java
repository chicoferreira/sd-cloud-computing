package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.common.logging.Logger;

public class ExitCommand extends AbstractCommand {

    private final Application application;

    public ExitCommand(Application application) {
        super("exit", "");
        this.application = application;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        application.exit();
    }
}
