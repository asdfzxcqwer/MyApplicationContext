package pl.slichota.cycle.exception;

public class DependencyGraphException extends RuntimeException {
    public DependencyGraphException(String message) {
        super(message);
    }
    public DependencyGraphException(String message, String context) {
        super(String.format("%s %s", message, context));
    }
}