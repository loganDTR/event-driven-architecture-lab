package it.zengfx.order.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderItemRequest(

        @NotBlank
        String productId,

        @Min(1)
        int quantity,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal unitPrice
) {
}