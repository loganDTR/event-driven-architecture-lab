package it.zengfx.order.adapter.out.kafka.message;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedMessage(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        UUID correlationId,
        UUID causationId,
        String aggregateId,
        OrderCreatedPayloadMessage payload
) {
}
