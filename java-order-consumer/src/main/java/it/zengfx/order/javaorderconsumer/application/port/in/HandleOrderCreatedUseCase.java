package it.zengfx.order.javaorderconsumer.application.port.in;

import it.zengfx.order.javaorderconsumer.domain.model.Order;

public interface HandleOrderCreatedUseCase {

    void handle(Order order);
}
