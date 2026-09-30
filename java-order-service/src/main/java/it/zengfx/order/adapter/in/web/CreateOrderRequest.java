package it.zengfx.order.adapter.in.web;

import it.zengfx.order.domain.model.SalesChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotBlank
        String orderId,

        @NotBlank
        String customerId,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        SalesChannel salesChannel,

        @NotEmpty
        List<@Valid CreateOrderItemRequest> items,

        UUID correlationId,

        UUID causationId
) {
}
