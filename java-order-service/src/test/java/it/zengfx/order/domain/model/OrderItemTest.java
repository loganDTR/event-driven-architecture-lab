package it.zengfx.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    void shouldRejectBlankProductId() {
        assertThatThrownBy(() -> new OrderItem(
                " ",
                1,
                BigDecimal.TEN
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productId cannot be null or blank");
    }

    @Test
    void shouldRejectQuantityLowerThanOne() {
        assertThatThrownBy(() -> new OrderItem(
                "PROD-101",
                0,
                BigDecimal.TEN
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity cannot be less than 1");
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
        assertThatThrownBy(() -> new OrderItem(
                "PROD-101",
                1,
                new BigDecimal("-0.01")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unitPrice cannot be negative");
    }
}
