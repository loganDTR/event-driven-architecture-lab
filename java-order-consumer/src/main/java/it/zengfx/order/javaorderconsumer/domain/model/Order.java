package it.zengfx.order.javaorderconsumer.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        String orderId,
        String customerId,
        String currency,
        BigDecimal totalAmount,
        List<OrderItem> items,
        OrderStatus status,
        Instant createdAt
) {
}
