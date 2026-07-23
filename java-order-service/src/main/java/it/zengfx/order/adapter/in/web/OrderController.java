package it.zengfx.order.adapter.in.web;

import it.zengfx.order.application.port.in.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderWebMapper mapper;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            OrderWebMapper mapper
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return createOrderUseCase
                .createOrder(mapper.toCommand(request))
                .thenApply(mapper::toResponse);
    }
}