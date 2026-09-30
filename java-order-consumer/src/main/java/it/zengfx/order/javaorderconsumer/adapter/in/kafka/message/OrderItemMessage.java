package it.zengfx.order.javaorderconsumer.adapter.in.kafka.message;

import java.math.BigDecimal;

public record OrderItemMessage(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
}
