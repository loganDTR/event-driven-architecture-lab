package it.zengfx.order.javaorderconsumer.application.port.out;

import it.zengfx.order.javaorderconsumer.domain.model.Order;

public interface SaveOrderPort {

    void save(Order order);
}
