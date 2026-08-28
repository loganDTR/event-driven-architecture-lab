package it.zengfx.order.adapter.in.web.error;

public class InvalidRequestDataException extends RuntimeException {

    public InvalidRequestDataException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}