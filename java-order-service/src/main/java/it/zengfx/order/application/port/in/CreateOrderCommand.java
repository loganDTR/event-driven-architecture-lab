package it.zengfx.order.application.port.in;

import it.zengfx.order.domain.event.OrderItem;
import it.zengfx.order.domain.event.SalesChannel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand (
        String orderId,
        String customerId,
        String currency,
        SalesChannel salesChannel,
        List<OrderItem> items,
        UUID correlationId,
        UUID causationId
){
    public BigDecimal calculateTotalAmount(){
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
