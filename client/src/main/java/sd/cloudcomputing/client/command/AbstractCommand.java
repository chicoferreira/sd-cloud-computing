package sd.cloudcomputing.client.command;

import sd.cloudcomputing.common.logging.Logger;

import java.util.OptionalInt;

public abstract class AbstractCommand implements Command {

    private final String name;
    private final String usage;

    public AbstractCommand(String name, String usage) {
        this.name = name;
        this.usage = usage;
    }

    public void sendUsage(Logger logger) {
        logger.info("Usage: " + getUsage());
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUsage() {
        return this.name + " " + this.usage;
    }

    protected OptionalInt parseInt(String arg) {
        try {
            return OptionalInt.of(Integer.parseInt(arg));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
