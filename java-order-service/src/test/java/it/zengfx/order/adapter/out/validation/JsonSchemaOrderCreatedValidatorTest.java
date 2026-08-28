package it.zengfx.order.adapter.out.validation;

import it.zengfx.order.application.exception.EventContractViolationException;
import it.zengfx.order.domain.event.OrderCreatedEvent;
import it.zengfx.order.domain.event.OrderCreatedPayload;
import it.zengfx.order.domain.event.OrderItem;
import it.zengfx.order.domain.event.SalesChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonSchemaOrderCreatedValidatorTest {

    private JsonSchemaOrderCreatedValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JsonSchemaOrderCreatedValidator(
                JsonMapper.builder()
                        .findAndAddModules()
                        .build()
        );
    }

    @Test
    void shouldAcceptValidOrderCreatedEvent() {
        OrderCreatedEvent event = createValidEvent();

        assertThatCode(() -> validator.validate(event))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectJsonWithoutRequiredCustomerId() {
        String invalidJson = """
                {
                  "eventId": "420a797f-99e6-4455-a756-f4e3c9f53bab",
                  "eventType": "order.created",
                  "eventVersion": 2,
                  "occurredAt": "2026-07-22T15:25:57.120Z",
                  "producer": "java-order-service",
                  "correlationId":
                    "0ef035ea-e428-437c-8bb2-5df84da91620",
                  "causationId": null,
                  "aggregateId": "ORD-1001",
                  "payload": {
                    "orderId": "ORD-1001",
                    "currency": "EUR",
                    "totalAmount": 19.90,
                    "salesChannel": "WEB",
                    "items": [
                      {
                        "productId": "PROD-101",
                        "quantity": 1,
                        "unitPrice": 19.90
                      }
                    ]
                  }
                }
                """;

        assertThatThrownBy(() -> validator.validateJson(invalidJson))
                .isInstanceOf(EventContractViolationException.class)
                .satisfies(exception -> {
                    EventContractViolationException violation =
                            (EventContractViolationException) exception;

                    assertThat(violation.violations())
                            .isNotEmpty()
                            .anyMatch(message ->
                                    message.contains("customerId")
                            );
                });
    }

    @Test
    void shouldRejectInvalidUuidFormat() {
        String invalidJson = """
                {
                  "eventId": "not-a-uuid",
                  "eventType": "order.created",
                  "eventVersion": 2,
                  "occurredAt": "2026-07-22T15:25:57.120Z",
                  "producer": "java-order-service",
                  "correlationId":
                    "0ef035ea-e428-437c-8bb2-5df84da91620",
                  "causationId": null,
                  "aggregateId": "ORD-1001",
                  "payload": {
                    "orderId": "ORD-1001",
                    "customerId": "CUS-501",
                    "currency": "EUR",
                    "totalAmount": 19.90,
                    "salesChannel": "WEB",
                    "items": [
                      {
                        "productId": "PROD-101",
                        "quantity": 1,
                        "unitPrice": 19.90
                      }
                    ]
                  }
                }
                """;

        assertThatThrownBy(() -> validator.validateJson(invalidJson))
                .isInstanceOf(EventContractViolationException.class)
                .satisfies(exception -> {
                    EventContractViolationException violation =
                            (EventContractViolationException) exception;

                    assertThat(violation.violations())
                            .anyMatch(message ->
                                    message.contains("eventId")
                                            || message.contains("uuid")
                            );
                });
    }

    private OrderCreatedEvent createValidEvent() {
        OrderCreatedPayload payload = new OrderCreatedPayload(
                "ORD-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("19.90"),
                SalesChannel.WEB,
                List.of(
                        new OrderItem(
                                "PROD-101",
                                1,
                                new BigDecimal("19.90")
                        )
                )
        );

        return OrderCreatedEvent.create(
                UUID.randomUUID(),
                null,
                payload
        );
    }
}