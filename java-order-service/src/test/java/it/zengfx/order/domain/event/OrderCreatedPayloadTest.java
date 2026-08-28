package it.zengfx.order.domain.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderCreatedPayloadTest {

    private final OrderItem item = new OrderItem(
            "PROD-101",
            2,
            new BigDecimal("19.90")
    );

    @Test
    void shouldCreateValidPayload() {
        OrderCreatedPayload payload = new OrderCreatedPayload(
                "ORD-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("39.80"),
                SalesChannel.WEB,
                List.of(item)
        );

        assertThat(payload.orderId()).isEqualTo("ORD-1001");
        assertThat(payload.currency()).isEqualTo("EUR");
        assertThat(payload.items()).hasSize(1);
    }

    @Test
    void shouldAllowMissingSalesChannel() {
        OrderCreatedPayload payload = new OrderCreatedPayload(
                "ORD-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("39.80"),
                null,
                List.of(item)
        );

        assertThat(payload.salesChannel()).isNull();
    }

    @Test
    void shouldRejectInvalidCurrency() {
        assertThatThrownBy(() -> new OrderCreatedPayload(
                "ORD-1001",
                "CUS-501",
                "euro",
                new BigDecimal("39.80"),
                SalesChannel.WEB,
                List.of(item)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "currency must be a valid ISO 4217 currency code"
                );
    }

    @Test
    void shouldRejectEmptyItems() {
        assertThatThrownBy(() -> new OrderCreatedPayload(
                "ORD-1001",
                "CUS-501",
                "EUR",
                BigDecimal.ZERO,
                SalesChannel.WEB,
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items cannot be null or empty");
    }

    @Test
    void shouldDefensivelyCopyItems() {
        var mutableItems = new java.util.ArrayList<>(
                List.of(item)
        );

        OrderCreatedPayload payload = new OrderCreatedPayload(
                "ORD-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("39.80"),
                SalesChannel.WEB,
                mutableItems
        );

        mutableItems.clear();

        assertThat(payload.items()).hasSize(1);

        assertThatThrownBy(() ->
                payload.items().add(item)
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}