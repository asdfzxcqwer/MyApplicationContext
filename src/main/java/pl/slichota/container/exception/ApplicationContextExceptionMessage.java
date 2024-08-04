package pl.slichota.container.exception;

public enum ApplicationContextExceptionMessage {
    BEAN_NOT_FOUND("Bean not found"),
    MULTIPLE_CONSTRUCTORS("Multiple constructors annotated with @Autowired"),
    CANNOT_CREATE_INSTANCE("Cannot create instance"),
    CYCLE_DETECTED("Dependency cycle detected");

    private final String message;

    ApplicationContextExceptionMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
