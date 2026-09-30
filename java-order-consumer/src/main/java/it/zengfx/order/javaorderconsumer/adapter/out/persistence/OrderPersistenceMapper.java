package it.zengfx.order.javaorderconsumer.adapter.out.persistence;

import it.zengfx.order.javaorderconsumer.domain.model.Order;

import java.time.ZoneOffset;

final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(order.orderId());
        entity.setCustomerId(order.customerId());
        entity.setStatus(order.status());
        entity.setTotalAmount(order.totalAmount());
        entity.setCreatedAt(order.createdAt().atOffset(ZoneOffset.UTC));
        return entity;
    }
}
