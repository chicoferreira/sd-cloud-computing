package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Application;
import sd.cloudcomputing.common.logging.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {

    private final Map<String, Command> commands;

    public CommandManager(Application application) {
        this.commands = new HashMap<>();

        register(new ConnectCommand(application));
        register(new HelpCommand(this));
        register(new JobCommand(application));
        register(new StatusCommand(application));
        register(new DisconnectCommand(application));
    }

    private void register(Command command) {
        this.commands.put(command.getName().toLowerCase(), command);
    }

    public Command getCommand(String command) {
        return this.commands.get(command.toLowerCase());
    }

    public Collection<Command> getAvailableCommands() {
        return this.commands.values();
    }

    public void handleCommand(Logger logger, String rawCommand) {
        String[] split = rawCommand.split(" ");
        if (split.length == 0) {
            return;
        }

        String commandName = split[0];
        String[] args = new String[split.length - 1];
        System.arraycopy(split, 1, args, 0, args.length);

        Command command = getCommand(commandName);
        if (command == null) {
            logger.info("Unknown command: " + commandName);
            return;
        }

        command.execute(logger, args);
    }

}
