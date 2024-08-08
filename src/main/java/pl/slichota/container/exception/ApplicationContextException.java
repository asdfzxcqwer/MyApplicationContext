package pl.slichota.container.exception;

import java.util.Arrays;

public class ApplicationContextException extends RuntimeException {
    public ApplicationContextException(String message) {
        super(message);
    }
    public ApplicationContextException(String message, String arg) {
        super(String.format("%s %s", message, arg));
    }
}
