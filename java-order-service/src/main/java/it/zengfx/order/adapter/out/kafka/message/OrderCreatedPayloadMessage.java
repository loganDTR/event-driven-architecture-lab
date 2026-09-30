package it.zengfx.order.adapter.out.kafka.message;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedPayloadMessage(
        String orderId,
        String customerId,
        String currency,
        BigDecimal totalAmount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String salesChannel,
        List<OrderItemMessage> items
) {
}
