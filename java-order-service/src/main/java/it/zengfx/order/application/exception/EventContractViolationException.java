package it.zengfx.order.application.exception;

import java.util.List;

public class EventContractViolationException extends RuntimeException {

    private final List<String> violations;

    public EventContractViolationException(
            String message,
            List<String> violations
    ) {
        super(message);
        this.violations = List.copyOf(violations);
    }

    public EventContractViolationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.violations = List.of();
    }

    public List<String> violations() {
        return violations;
    }
}