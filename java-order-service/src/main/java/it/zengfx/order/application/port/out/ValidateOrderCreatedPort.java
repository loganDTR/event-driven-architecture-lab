package it.zengfx.order.application.port.out;

import it.zengfx.order.domain.event.OrderCreatedEvent;

public interface ValidateOrderCreatedPort {

    void validate(OrderCreatedEvent event);
}