package it.zengfx.order.application.service;

import it.zengfx.order.application.port.in.CreateOrderCommand;
import it.zengfx.order.application.port.in.CreateOrderResult;
import it.zengfx.order.application.port.in.CreateOrderUseCase;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.domain.model.Order;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CreateOrderService implements CreateOrderUseCase {

    private final PublishOrderCreatedPort eventPublisher;

    public CreateOrderService(
            PublishOrderCreatedPort eventPublisher
    ) {
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher,
                "eventPublisher cannot be null"
        );
    }

    @Override
    public CompletableFuture<CreateOrderResult> createOrder(CreateOrderCommand command) {
        Objects.requireNonNull(command, "CreateOrderCommand must not be null");

        Order order = new Order(
                command.orderId(),
                command.customerId(),
                command.currency(),
                command.salesChannel(),
                command.items()
        );

        UUID correlationId = command.correlationId() != null
                ? command.correlationId()
                : UUID.randomUUID();

        return eventPublisher.publish(
                        order,
                        correlationId,
                        command.causationId()
                )
                .thenApply(publication -> new CreateOrderResult(
                        order.orderId(),
                        publication.eventId(),
                        order.totalAmount()
                ));
    }
}
