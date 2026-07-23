package it.zengfx.order.domain.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record OrderCreatedPayload (
        String orderId,
        String customerId,
        String currency,
        BigDecimal totalAmount,
        SalesChannel salesChannel,
        List<OrderItem> items
){
    private static final String CURRENCY_PATTERN = "^[A-Z]{3}$";
    public OrderCreatedPayload {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("orderId cannot be null or empty");
        }
        if (customerId == null || customerId.isEmpty()) {
            throw new IllegalArgumentException("customerId cannot be null or empty");
        }
        if (currency == null || !currency.matches(CURRENCY_PATTERN)) {
            throw new IllegalArgumentException("currency must be a valid ISO 4217 currency code");
        }
        Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("totalAmount cannot be negative");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be null or empty");
        }
        items = List.copyOf(items);
    }
}
