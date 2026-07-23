package it.zengfx.order.application.port.out;

import it.zengfx.order.domain.event.OrderCreatedEvent;

import java.util.concurrent.CompletableFuture;

public interface PublishOrderCreatedPort {
    CompletableFuture<Void> publish (OrderCreatedEvent event);
}
