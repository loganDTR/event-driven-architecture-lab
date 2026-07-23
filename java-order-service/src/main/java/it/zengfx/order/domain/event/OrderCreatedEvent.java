package it.zengfx.order.domain.event;

import it.zengfx.order.domain.model.Order;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record OrderCreatedEvent (
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        UUID correlationId,
        UUID causationId,
        String aggregateId,
        OrderCreatedPayload payload
){
    public static final String EVENT_TYPE = "order.created";
    public static final int EVENT_VERSION = 2;
    private static final Pattern PRODUCER_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    public OrderCreatedEvent{
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(
                correlationId,
                "correlationId must not be null"
        );
        Objects.requireNonNull(payload, "payload must not be null");

        if (!EVENT_TYPE.equals(eventType)) {
            throw new IllegalArgumentException(
                    "eventType must be " + EVENT_TYPE
            );
        }

        if (eventVersion < 1) {
            throw new IllegalArgumentException(
                    "eventVersion must be greater than or equal to 1"
            );
        }

        if (producer == null
                || !PRODUCER_PATTERN.matcher(producer).matches()) {
            throw new IllegalArgumentException(
                    "producer must match ^[a-z][a-z0-9-]*$"
            );
        }

        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException(
                    "aggregateId must not be blank"
            );
        }
    }

    public static OrderCreatedEvent create(
            UUID correlationId,
            UUID causationId,
            OrderCreatedPayload payload
    ){
        Objects.requireNonNull(payload, "payload must not be null");
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                "java-order-service",
                correlationId,
                causationId,
                payload.orderId(),
                payload
        );
    }
}
