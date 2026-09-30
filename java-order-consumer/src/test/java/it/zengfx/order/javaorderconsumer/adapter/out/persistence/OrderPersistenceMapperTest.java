package it.zengfx.order.javaorderconsumer.adapter.out.persistence;

import it.zengfx.order.javaorderconsumer.domain.model.Order;
import it.zengfx.order.javaorderconsumer.domain.model.OrderItem;
import it.zengfx.order.javaorderconsumer.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderPersistenceMapperTest {

    @Test
    void mapsOnlyTheConsumerProjectionToJpaEntity() {
        Instant occurredAt = Instant.parse("2026-09-30T12:00:00Z");
        Order order = new Order(
                "ORD-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("50.30"),
                List.of(new OrderItem("PROD-101", 2, new BigDecimal("25.15"))),
                OrderStatus.CREATED,
                occurredAt
        );

        OrderEntity entity = OrderPersistenceMapper.toEntity(order);

        assertNull(entity.getId());
        assertEquals("ORD-1001", entity.getOrderId());
        assertEquals("CUS-501", entity.getCustomerId());
        assertEquals(OrderStatus.CREATED, entity.getStatus());
        assertEquals(new BigDecimal("50.30"), entity.getTotalAmount());
        assertEquals(occurredAt.atOffset(ZoneOffset.UTC), entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }
}
