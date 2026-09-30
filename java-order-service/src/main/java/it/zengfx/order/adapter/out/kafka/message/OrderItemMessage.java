package it.zengfx.order.adapter.out.kafka.message;

import java.math.BigDecimal;

public record OrderItemMessage(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
}
