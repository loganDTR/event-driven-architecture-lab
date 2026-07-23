package it.zengfx.order.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderResult(
        String orderId,
        UUID eventId,
        BigDecimal totalAmount
) {
}
