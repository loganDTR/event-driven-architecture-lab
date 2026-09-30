package it.zengfx.order.javaorderconsumer.adapter.in.kafka;

import it.zengfx.order.javaorderconsumer.adapter.in.kafka.message.OrderCreatedMessage;
import it.zengfx.order.javaorderconsumer.domain.model.Order;
import it.zengfx.order.javaorderconsumer.domain.model.OrderItem;
import it.zengfx.order.javaorderconsumer.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class OrderCreatedMessageMapper {

    private static final String ORDER_CREATED_EVENT_TYPE = "order.created";
    private static final int SUPPORTED_EVENT_VERSION = 2;

    public Order toDomain(OrderCreatedMessage message) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(message.payload(), "message.payload cannot be null");
        Objects.requireNonNull(message.occurredAt(), "message.occurredAt cannot be null");

        if (!ORDER_CREATED_EVENT_TYPE.equals(message.eventType())) {
            throw new IllegalArgumentException("Unsupported eventType: " + message.eventType());
        }

        if (message.eventVersion() != SUPPORTED_EVENT_VERSION) {
            throw new IllegalArgumentException("Unsupported eventVersion: " + message.eventVersion());
        }

        if (!Objects.equals(message.aggregateId(), message.payload().orderId())) {
            throw new IllegalArgumentException("aggregateId must match payload.orderId");
        }

        var items = Objects.requireNonNull(message.payload().items(), "payload.items cannot be null")
                .stream()
                .map(item -> new OrderItem(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        return new Order(
                message.payload().orderId(),
                message.payload().customerId(),
                message.payload().currency(),
                message.payload().totalAmount(),
                items,
                OrderStatus.CREATED,
                message.occurredAt()
        );
    }
}
