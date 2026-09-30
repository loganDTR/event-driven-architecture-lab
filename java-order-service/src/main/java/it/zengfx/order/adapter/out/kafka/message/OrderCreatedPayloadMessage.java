package it.zengfx.order.adapter.out.kafka.message;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedPayloadMessage(
        String orderId,
        String customerId,
        String currency,
        BigDecimal totalAmount,
        String salesChannel,
        List<OrderItemMessage> items
) {
}
