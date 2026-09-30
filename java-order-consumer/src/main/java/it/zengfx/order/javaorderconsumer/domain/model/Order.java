package it.zengfx.order.javaorderconsumer.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Order(
        String orderId,
        String customerId,
        String currency,
        BigDecimal totalAmount,
        List<OrderItem> items,
        OrderStatus status,
        Instant createdAt
) {
    private static final String CURRENCY_PATTERN = "^[A-Z]{3}$";

    public Order {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId cannot be null or blank");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId cannot be null or blank");
        }
        if (currency == null || !currency.matches(CURRENCY_PATTERN)) {
            throw new IllegalArgumentException("currency must be a valid ISO 4217 currency code");
        }

        Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalAmount cannot be negative");
        }

        Objects.requireNonNull(items, "items cannot be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be empty");
        }
        items = List.copyOf(items);

        BigDecimal calculatedTotal = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (calculatedTotal.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("totalAmount must match the sum of order items");
        }

        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }
}
