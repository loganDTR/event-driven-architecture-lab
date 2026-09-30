package it.zengfx.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void shouldCalculateTotalAmountFromItems() {
        Order order = validOrder(List.of(
                new OrderItem("PROD-101", 2, new BigDecimal("19.90")),
                new OrderItem("PROD-202", 1, new BigDecimal("10.50"))
        ));

        assertThat(order.totalAmount()).isEqualByComparingTo("50.30");
    }

    @Test
    void shouldRejectEmptyItems() {
        assertThatThrownBy(() -> validOrder(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items cannot be empty");
    }

    @Test
    void shouldDefensivelyCopyItems() {
        var mutableItems = new ArrayList<>(
                List.of(new OrderItem("PROD-101", 1, new BigDecimal("19.90")))
        );

        Order order = validOrder(mutableItems);
        mutableItems.clear();

        assertThat(order.items()).hasSize(1);
        assertThatThrownBy(() -> order.items().add(
                new OrderItem("PROD-202", 1, BigDecimal.TEN)
        )).isInstanceOf(UnsupportedOperationException.class);
    }

    private static Order validOrder(List<OrderItem> items) {
        return new Order(
                "ORD-1001",
                "CUS-501",
                "EUR",
                SalesChannel.WEB,
                items
        );
    }
}
