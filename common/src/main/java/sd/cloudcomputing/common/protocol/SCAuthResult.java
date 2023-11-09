package sd.cloudcomputing.common.protocol;

import org.jetbrains.annotations.NotNull;
import sd.cloudcomputing.common.serialization.*;
import sd.cloudcomputing.common.util.AuthenticateResult;

import java.io.IOException;

public record SCAuthResult(AuthenticateResult result) {

    public boolean isSuccess() {
        return result == AuthenticateResult.LOGGED_IN || result == AuthenticateResult.REGISTERED;
    }

    public static class Serialization implements Serialize<SCAuthResult> {

        @Override
        public @NotNull SCAuthResult deserialize(SerializeInput input, Frost frost) throws SerializationException, IOException {
            return switch (frost.readInt(input)) {
                case 0 -> new SCAuthResult(AuthenticateResult.LOGGED_IN);
                case 1 -> new SCAuthResult(AuthenticateResult.WRONG_PASSWORD);
                case 2 -> new SCAuthResult(AuthenticateResult.REGISTERED);
                default -> throw new SerializationException("Invalid auth result");
            };
        }

        @Override
        public void serialize(SCAuthResult object, SerializeOutput output, Frost frost) throws IOException {
            frost.writeInt(switch (object.result) {
                case LOGGED_IN -> 0;
                case WRONG_PASSWORD -> 1;
                case REGISTERED -> 2;
            }, output);
        }
    }

}
