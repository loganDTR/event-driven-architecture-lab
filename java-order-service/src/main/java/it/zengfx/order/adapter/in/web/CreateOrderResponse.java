package it.zengfx.order.adapter.in.web;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderResponse(
        String orderId,
        UUID eventId,
        BigDecimal totalAmount,
        String status
) {
}