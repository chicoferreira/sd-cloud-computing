package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;

public record SCAuthResult(Result result) {

    enum Result {
        LOGGED_IN,
        WRONG_PASSWORD,
        REGISTERED,
    }

    public boolean isSuccess() {
        return result == Result.LOGGED_IN || result == Result.REGISTERED;
    }

    public static class Serialization implements Serialize<SCAuthResult> {

        @Override
        public @NotNull SCAuthResult deserialize(SerializeInput input, Frost frost) throws SerializationException {
            return switch (frost.readInt(input)) {
                case 0 -> new SCAuthResult(Result.LOGGED_IN);
                case 1 -> new SCAuthResult(Result.WRONG_PASSWORD);
                case 2 -> new SCAuthResult(Result.REGISTERED);
                default -> throw new SerializationException("Invalid auth result");
            };
        }

        @Override
        public void serialize(SCAuthResult object, SerializeOutput output, Frost frost) throws SerializationException {
            frost.writeInt(switch (object.result) {
                case LOGGED_IN -> 0;
                case WRONG_PASSWORD -> 1;
                case REGISTERED -> 2;
            }, output);
        }
    }

}
