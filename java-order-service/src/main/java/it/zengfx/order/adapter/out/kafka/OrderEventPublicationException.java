package it.zengfx.order.adapter.out.kafka;

public class OrderEventPublicationException extends RuntimeException {
    public OrderEventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
