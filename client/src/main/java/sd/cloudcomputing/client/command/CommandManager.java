package sd.cloudcomputing.client.command;

import sd.cloudcomputing.client.Client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {

    private final Map<String, Command> commands;

    public CommandManager(Client client) {
        this.commands = new HashMap<>();

        register(new ConnectCommand(client));
        register(new HelpCommand(this));
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
}
