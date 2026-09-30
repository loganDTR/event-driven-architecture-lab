package it.zengfx.order.adapter.out.kafka;

import it.zengfx.order.domain.model.Order;
import it.zengfx.order.domain.model.OrderItem;
import it.zengfx.order.domain.model.SalesChannel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCreatedMessageMapperTest {

    private final OrderCreatedMessageMapper mapper =
            new OrderCreatedMessageMapper();

    @Test
    void shouldMapDomainOrderToV2IntegrationMessage() {
        UUID correlationId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();

        var message = mapper.toMessage(
                order("ORD-1001", "CUS-501", "50.30"),
                correlationId,
                causationId
        );

        assertThat(message.eventId()).isNotNull();
        assertThat(message.eventType()).isEqualTo("order.created");
        assertThat(message.eventVersion()).isEqualTo(2);
        assertThat(message.occurredAt()).isNotNull();
        assertThat(message.producer()).isEqualTo("java-order-service");
        assertThat(message.correlationId()).isEqualTo(correlationId);
        assertThat(message.causationId()).isEqualTo(causationId);
        assertThat(message.aggregateId()).isEqualTo("ORD-1001");
        assertThat(message.payload().orderId()).isEqualTo("ORD-1001");
        assertThat(message.payload().customerId()).isEqualTo("CUS-501");
        assertThat(message.payload().currency()).isEqualTo("EUR");
        assertThat(message.payload().totalAmount())
                .isEqualByComparingTo("50.30");
        assertThat(message.payload().salesChannel()).isEqualTo("WEB");
        assertThat(message.payload().items()).hasSize(1);
    }

    private static Order order(
            String orderId,
            String customerId,
            String amount
    ) {
        BigDecimal total = new BigDecimal(amount);

        return new Order(
                orderId,
                customerId,
                "EUR",
                SalesChannel.WEB,
                List.of(new OrderItem("PROD-TEST", 1, total))
        );
    }
}
