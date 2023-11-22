package sd.cloudcomputing.client.command;

import sd.cloudcomputing.common.logging.Logger;

import java.util.Comparator;

public class HelpCommand extends AbstractCommand {

    private final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        super("help", "");
        this.commandManager = commandManager;
    }

    @Override
    public void execute(Logger logger, String[] args) {
        logger.info("Available commands:");
        commandManager.getAvailableCommands()
                .stream()
                .sorted(Comparator.comparing(Command::getName))
                .forEach(command -> logger.info(" - " + command.getUsage()));
    }
}
