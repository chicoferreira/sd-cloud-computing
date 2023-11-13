package sd.cloudcomputing.common.util;

public enum AuthenticateResult {
    LOGGED_IN,
    WRONG_PASSWORD,
    REGISTERED;

    public boolean isSuccess() {
        return this == LOGGED_IN || this == REGISTERED;
    }

}
