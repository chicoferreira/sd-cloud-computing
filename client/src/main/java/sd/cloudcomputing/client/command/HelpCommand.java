package sd.cloudcomputing.client.command;

import sd.cloudcomputing.common.logging.Logger;

public class HelpCommand extends AbstractCommand {

    private final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        super("help", "");
        this.commandManager = commandManager;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        logger.info("Available commands:");
        for (Command command : commandManager.getAvailableCommands()) {
            logger.info(" - " + command.getUsage());
        }
    }
}
