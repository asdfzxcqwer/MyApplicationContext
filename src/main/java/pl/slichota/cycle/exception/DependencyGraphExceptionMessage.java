package pl.slichota.cycle.exception;

public enum DependencyGraphExceptionMessage {
    CYCLE_DETECTED("Cycle has been detected"),
    IT_IS_NOT_BEAN("");
    private final String message;

    DependencyGraphExceptionMessage(String message) {
        this.message = message;
    }
    public String getMessage() {
        return this.message;
    }
}
