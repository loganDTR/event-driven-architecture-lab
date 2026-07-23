package it.zengfx.order.application.service;

import it.zengfx.order.application.port.in.CreateOrderCommand;
import it.zengfx.order.application.port.in.CreateOrderResult;
import it.zengfx.order.application.port.in.CreateOrderUseCase;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.domain.event.OrderCreatedEvent;
import it.zengfx.order.domain.event.OrderCreatedPayload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final PublishOrderCreatedPort eventPublisher;

    public CreateOrderService(PublishOrderCreatedPort eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CompletableFuture<CreateOrderResult> createOrder(CreateOrderCommand command) {
        Objects.requireNonNull(command, "CreateOrderCommand must not be null");

        BigDecimal totalAmount = command.calculateTotalAmount();

        OrderCreatedPayload payload = new OrderCreatedPayload(
                command.orderId(),
                command.customerId(),
                command.currency(),
                totalAmount,
                command.salesChannel(),
                command.items()
        );

        UUID correlationId = command.correlationId() != null ? command.correlationId() : UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.create(
                correlationId,
                command.causationId(),
                payload
        );

        return eventPublisher.publish(event).thenApply(ignored -> new CreateOrderResult(
                payload.orderId(),
                event.eventId(),
                totalAmount
        ));
    }
}
