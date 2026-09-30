package it.zengfx.order.javaorderconsumer.adapter.in.kafka;

import it.zengfx.order.javaorderconsumer.adapter.in.kafka.message.OrderCreatedMessage;
import it.zengfx.order.javaorderconsumer.adapter.in.kafka.message.OrderCreatedPayloadMessage;
import it.zengfx.order.javaorderconsumer.adapter.in.kafka.message.OrderItemMessage;
import it.zengfx.order.javaorderconsumer.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderCreatedMessageMapperTest {

    private final OrderCreatedMessageMapper mapper = new OrderCreatedMessageMapper();

    @Test
    void mapsV2MessageToDomainOrder() {
        Instant occurredAt = Instant.parse("2026-09-30T12:00:00Z");
        OrderCreatedMessage message = message("ORD-1001", "ORD-1001", 2, occurredAt);

        var order = mapper.toDomain(message);

        assertEquals("ORD-1001", order.orderId());
        assertEquals("CUS-501", order.customerId());
        assertEquals("EUR", order.currency());
        assertEquals(new BigDecimal("50.30"), order.totalAmount());
        assertEquals(OrderStatus.CREATED, order.status());
        assertEquals(occurredAt, order.createdAt());
        assertEquals(2, order.items().size());
    }

    @Test
    void rejectsUnsupportedEventVersion() {
        OrderCreatedMessage message = message(
                "ORD-1001",
                "ORD-1001",
                1,
                Instant.parse("2026-09-30T12:00:00Z")
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(message));
    }

    @Test
    void rejectsTotalAmountDifferentFromItemsSum() {
        OrderCreatedMessage validMessage = message(
                "ORD-1001",
                "ORD-1001",
                2,
                Instant.parse("2026-09-30T12:00:00Z")
        );

        var invalidPayload = new OrderCreatedPayloadMessage(
                validMessage.payload().orderId(),
                validMessage.payload().customerId(),
                validMessage.payload().currency(),
                new BigDecimal("99.99"),
                validMessage.payload().salesChannel(),
                validMessage.payload().items()
        );

        var invalidMessage = new OrderCreatedMessage(
                validMessage.eventId(),
                validMessage.eventType(),
                validMessage.eventVersion(),
                validMessage.occurredAt(),
                validMessage.producer(),
                validMessage.correlationId(),
                validMessage.causationId(),
                validMessage.aggregateId(),
                invalidPayload
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(invalidMessage));
    }

    @Test
    void rejectsAggregateIdDifferentFromOrderId() {
        OrderCreatedMessage message = message(
                "OTHER-ORDER",
                "ORD-1001",
                2,
                Instant.parse("2026-09-30T12:00:00Z")
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(message));
    }

    private static OrderCreatedMessage message(
            String aggregateId,
            String orderId,
            int eventVersion,
            Instant occurredAt
    ) {
        return new OrderCreatedMessage(
                UUID.randomUUID(),
                "order.created",
                eventVersion,
                occurredAt,
                "java-order-service",
                UUID.randomUUID(),
                null,
                aggregateId,
                new OrderCreatedPayloadMessage(
                        orderId,
                        "CUS-501",
                        "EUR",
                        new BigDecimal("50.30"),
                        "WEB",
                        List.of(
                                new OrderItemMessage("PROD-101", 2, new BigDecimal("19.90")),
                                new OrderItemMessage("PROD-202", 1, new BigDecimal("10.50"))
                        )
                )
        );
    }
}
