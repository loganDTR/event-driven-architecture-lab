package it.zengfx.order.application.port.in;

import java.util.concurrent.CompletableFuture;

public interface CreateOrderUseCase {
    CompletableFuture<CreateOrderResult> createOrder(CreateOrderCommand command);
}
