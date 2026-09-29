package it.zengfx.order.adapter.in.web;

import it.zengfx.order.adapter.in.web.error.GlobalExceptionHandler;
import it.zengfx.order.application.exception.EventPublicationException;
import it.zengfx.order.application.port.in.CreateOrderCommand;
import it.zengfx.order.application.port.in.CreateOrderResult;
import it.zengfx.order.application.port.in.CreateOrderUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({
        OrderWebMapper.class,
        GlobalExceptionHandler.class
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOrderUseCase createOrderUseCase;

    @Test
    void shouldReturn201WhenOrderIsCreated() throws Exception {
        UUID eventId = UUID.randomUUID();

        CreateOrderResult result = new CreateOrderResult(
                "ORD-1001",
                eventId,
                new BigDecimal("50.30")
        );

        when(createOrderUseCase.createOrder(any(CreateOrderCommand.class)))
                .thenReturn(CompletableFuture.completedFuture(result));

        MvcResult asyncResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.orderId")
                        .value("ORD-1001"))
                .andExpect(jsonPath("$.eventId")
                        .value(eventId.toString()))
                .andExpect(jsonPath("$.totalAmount")
                        .value(50.30))
                .andExpect(jsonPath("$.status")
                        .value("PUBLISHED"));

        verify(createOrderUseCase)
                .createOrder(any(CreateOrderCommand.class));
    }

    @Test
    void shouldMapHttpRequestToApplicationCommand() throws Exception {
        UUID correlationId =
                UUID.fromString("0ef035ea-e428-437c-8bb2-5df84da91620");

        CreateOrderResult result = new CreateOrderResult(
                "ORD-1001",
                UUID.randomUUID(),
                new BigDecimal("50.30")
        );

        when(createOrderUseCase.createOrder(any(CreateOrderCommand.class)))
                .thenReturn(CompletableFuture.completedFuture(result));

        MvcResult asyncResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isCreated());

        verify(createOrderUseCase).createOrder(
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.orderId().equals("ORD-1001")
                                && command.customerId().equals("CUS-501")
                                && command.currency().equals("EUR")
                                && command.salesChannel().name().equals("WEB")
                                && command.items().size() == 2
                                && command.items().getFirst()
                                .productId().equals("PROD-101")
                                && command.items().getFirst()
                                .quantity() == 2
                                && command.items().getFirst()
                                .unitPrice()
                                .compareTo(
                                        new BigDecimal("19.90")
                                ) == 0
                                && correlationId.equals(
                                command.correlationId()
                        )
                                && command.causationId() == null
                )
        );
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        String invalidRequest = """
                {
                  "orderId": "",
                  "customerId": "",
                  "currency": "EURO",
                  "items": []
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors")
                        .isArray())
                .andExpect(jsonPath("$.errors.length()")
                        .value(4));

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void shouldReturn400WhenItemIsInvalid() throws Exception {
        String invalidRequest = """
                {
                  "orderId": "ORD-1001",
                  "customerId": "CUS-501",
                  "currency": "EUR",
                  "salesChannel": "WEB",
                  "items": [
                    {
                      "productId": "",
                      "quantity": 0,
                      "unitPrice": -0.01
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors")
                        .isArray());

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void shouldReturn503WhenEventPublicationFails() throws Exception {
        EventPublicationException failure =
                new EventPublicationException(
                        "Unable to publish OrderCreated event",
                        new RuntimeException("Kafka unavailable")
                );

        when(createOrderUseCase.createOrder(any(CreateOrderCommand.class)))
                .thenReturn(CompletableFuture.failedFuture(failure));

        MvcResult asyncResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Event publication unavailable"))
                .andExpect(jsonPath("$.status")
                        .value(503))
                .andExpect(jsonPath("$.code")
                        .value("EVENT_PUBLICATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "Il servizio non è temporaneamente "
                                        + "in grado di pubblicare l'evento."
                        ))
                .andExpect(jsonPath("$.instance")
                        .value("/api/orders"));
    }

    @Test
    void shouldReturn500ForUnexpectedFailure() throws Exception {
        RuntimeException failure =
                new RuntimeException("Unexpected internal failure");

        when(createOrderUseCase.createOrder(any(CreateOrderCommand.class)))
                .thenReturn(CompletableFuture.failedFuture(failure));

        MvcResult asyncResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Internal server error"))
                .andExpect(jsonPath("$.status")
                        .value(500))
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "Si è verificato un errore interno inatteso."
                        ))
                .andExpect(jsonPath("$.instance")
                        .value("/api/orders"));
    }

    @Test
    void shouldReturn400ForMalformedJson() throws Exception {
        String malformedJson = """
                {
                  "orderId": "ORD-1001",
                  "customerId": "CUS-501",
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createOrderUseCase);
    }

    private String validRequest() {
        return """
                {
                  "orderId": "ORD-1001",
                  "customerId": "CUS-501",
                  "currency": "EUR",
                  "salesChannel": "WEB",
                  "items": [
                    {
                      "productId": "PROD-101",
                      "quantity": 2,
                      "unitPrice": 19.90
                    },
                    {
                      "productId": "PROD-202",
                      "quantity": 1,
                      "unitPrice": 10.50
                    }
                  ],
                  "correlationId":
                    "0ef035ea-e428-437c-8bb2-5df84da91620",
                  "causationId": null
                }
                """;
    }
}