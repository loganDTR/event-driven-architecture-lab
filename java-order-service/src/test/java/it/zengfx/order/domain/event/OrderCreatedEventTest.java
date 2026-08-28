package it.zengfx.order.domain.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderCreatedEventTest {

    @Test
    void shouldCreateOrderCreatedEventWithStandardMetadata() {
        UUID correlationId = UUID.randomUUID();

        OrderCreatedPayload payload = new OrderCreatedPayload(
                "ORD-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("19.90"),
                SalesChannel.WEB,
                List.of(
                        new OrderItem(
                                "PROD-101",
                                1,
                                new BigDecimal("19.90")
                        )
                )
        );

        OrderCreatedEvent event = OrderCreatedEvent.create(
                correlationId,
                null,
                payload
        );

        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType())
                .isEqualTo("order.created");
        assertThat(event.eventVersion()).isEqualTo(2);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.producer())
                .isEqualTo("java-order-service");
        assertThat(event.correlationId())
                .isEqualTo(correlationId);
        assertThat(event.causationId()).isNull();
        assertThat(event.aggregateId())
                .isEqualTo("ORD-1001");
        assertThat(event.payload()).isSameAs(payload);
    }
}