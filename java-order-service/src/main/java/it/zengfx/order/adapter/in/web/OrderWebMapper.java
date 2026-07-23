package it.zengfx.order.adapter.in.web;

import it.zengfx.order.application.port.in.CreateOrderCommand;
import it.zengfx.order.application.port.in.CreateOrderResult;
import it.zengfx.order.domain.event.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderWebMapper {

    public CreateOrderCommand toCommand(CreateOrderRequest request) {
        var items = request.items().stream()
                .map(item -> new OrderItem(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        return new CreateOrderCommand(
                request.orderId(),
                request.customerId(),
                request.currency(),
                request.salesChannel(),
                items,
                request.correlationId(),
                request.causationId()
        );
    }

    public CreateOrderResponse toResponse(CreateOrderResult result) {
        return new CreateOrderResponse(
                result.orderId(),
                result.eventId(),
                result.totalAmount(),
                "PUBLISHED"
        );
    }
}