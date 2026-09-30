package it.zengfx.order.adapter.out.kafka;

import it.zengfx.order.adapter.out.kafka.message.OrderCreatedMessage;
import it.zengfx.order.adapter.out.kafka.message.OrderCreatedPayloadMessage;
import it.zengfx.order.adapter.out.kafka.message.OrderItemMessage;
import it.zengfx.order.domain.model.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class OrderCreatedMessageMapper {

    static final String EVENT_TYPE = "order.created";
    static final int EVENT_VERSION = 2;
    static final String PRODUCER = "java-order-service";

    public OrderCreatedMessage toMessage(
            Order order,
            UUID correlationId,
            UUID causationId
    ) {
        Objects.requireNonNull(order, "order cannot be null");
        Objects.requireNonNull(correlationId, "correlationId cannot be null");

        var items = order.items().stream()
                .map(item -> new OrderItemMessage(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        var payload = new OrderCreatedPayloadMessage(
                order.orderId(),
                order.customerId(),
                order.currency(),
                order.totalAmount(),
                order.salesChannel() == null
                        ? null
                        : order.salesChannel().name(),
                items
        );

        return new OrderCreatedMessage(
                UUID.randomUUID(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                PRODUCER,
                correlationId,
                causationId,
                order.orderId(),
                payload
        );
    }
}
