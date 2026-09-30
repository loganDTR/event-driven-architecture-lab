package it.zengfx.order.application.port.in;

import it.zengfx.order.domain.model.OrderItem;
import it.zengfx.order.domain.model.SalesChannel;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        String orderId,
        String customerId,
        String currency,
        SalesChannel salesChannel,
        List<OrderItem> items,
        UUID correlationId,
        UUID causationId
) {
}
