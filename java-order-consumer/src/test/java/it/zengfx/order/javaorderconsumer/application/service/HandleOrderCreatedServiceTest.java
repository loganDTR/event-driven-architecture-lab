package it.zengfx.order.javaorderconsumer.application.service;

import it.zengfx.order.javaorderconsumer.application.port.out.SaveOrderPort;
import it.zengfx.order.javaorderconsumer.domain.model.Order;
import it.zengfx.order.javaorderconsumer.domain.model.OrderItem;
import it.zengfx.order.javaorderconsumer.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HandleOrderCreatedServiceTest {

    @Test
    void delegatesPersistenceToOutputPort() {
        SaveOrderPort saveOrderPort = mock(SaveOrderPort.class);
        HandleOrderCreatedService service = new HandleOrderCreatedService(saveOrderPort);

        Order order = new Order(
                "ORD-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("50.30"),
                List.of(
                        new OrderItem("PROD-101", 2, new BigDecimal("19.90")),
                        new OrderItem("PROD-202", 1, new BigDecimal("10.50"))
                ),
                OrderStatus.CREATED,
                Instant.parse("2026-09-30T12:00:00Z")
        );

        service.handle(order);

        verify(saveOrderPort).save(order);
    }
}
