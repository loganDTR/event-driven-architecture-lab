package it.zengfx.order.application.exception;

public class EventPublicationException extends RuntimeException {
    public EventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
