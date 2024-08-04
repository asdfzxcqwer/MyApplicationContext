package pl.slichota.container.exception;

public enum ApplicationContextMessage {
    BEAN_NOT_FOUND("Bean not found"),
    CYCLE_DETECTED("Cycle has been detected");

    private final String message;

    ApplicationContextMessage(String message) {
        this.message = message;
    }
    public String getMessage() {
        return this.message;
    }
}
