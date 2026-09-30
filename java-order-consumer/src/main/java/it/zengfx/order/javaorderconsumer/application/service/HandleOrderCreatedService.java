package it.zengfx.order.javaorderconsumer.application.service;

import it.zengfx.order.javaorderconsumer.application.port.in.HandleOrderCreatedUseCase;
import it.zengfx.order.javaorderconsumer.application.port.out.SaveOrderPort;
import it.zengfx.order.javaorderconsumer.domain.model.Order;

import java.util.Objects;

public final class HandleOrderCreatedService implements HandleOrderCreatedUseCase {

    private final SaveOrderPort saveOrderPort;

    public HandleOrderCreatedService(SaveOrderPort saveOrderPort) {
        this.saveOrderPort = Objects.requireNonNull(saveOrderPort, "saveOrderPort cannot be null");
    }

    @Override
    public void handle(Order order) {
        saveOrderPort.save(Objects.requireNonNull(order, "order cannot be null"));
    }
}
