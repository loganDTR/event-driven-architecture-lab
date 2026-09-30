package it.zengfx.order.application.port.out;

import it.zengfx.order.domain.model.Order;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PublishOrderCreatedPort {

    CompletableFuture<PublishOrderCreatedResult> publish(
            Order order,
            UUID correlationId,
            UUID causationId
    );
}
