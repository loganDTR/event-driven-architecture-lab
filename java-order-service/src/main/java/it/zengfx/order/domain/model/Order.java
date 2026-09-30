package it.zengfx.order.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record Order(
        String orderId,
        String customerId,
        String currency,
        SalesChannel salesChannel,
        List<OrderItem> items
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

        Objects.requireNonNull(items, "items cannot be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items cannot be empty");
        }
        items = List.copyOf(items);
    }

    public BigDecimal totalAmount() {
        return items.stream()
                .map(item -> item.unitPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
