package it.zengfx.order.application.port.out;

import java.util.UUID;

public record PublishOrderCreatedResult(
        UUID eventId
) {
}
