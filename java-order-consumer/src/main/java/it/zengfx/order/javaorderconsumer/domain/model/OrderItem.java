package it.zengfx.order.javaorderconsumer.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record OrderItem(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
    public OrderItem{
        if (productId == null || productId.isBlank()){
            throw new IllegalArgumentException("productId cannot be null or blank");
        }
        if (quantity < 1){
            throw new IllegalArgumentException("quantity cannot be less than 1");
        }
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("unitPrice cannot be negative");
        }
    }
}
