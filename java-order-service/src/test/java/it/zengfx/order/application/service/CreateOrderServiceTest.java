package it.zengfx.order.application.service;

import it.zengfx.order.application.port.in.CreateOrderCommand;
import it.zengfx.order.application.port.in.CreateOrderResult;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.application.port.out.PublishOrderCreatedResult;
import it.zengfx.order.domain.model.Order;
import it.zengfx.order.domain.model.OrderItem;
import it.zengfx.order.domain.model.SalesChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private PublishOrderCreatedPort eventPublisher;

    private CreateOrderService service;

    @BeforeEach
    void setUp() {
        service = new CreateOrderService(eventPublisher);
    }

    @Test
    void shouldCreateDomainOrderAndPublishIt() {
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        CreateOrderCommand command = createCommand(correlationId, null);

        when(eventPublisher.publish(
                any(Order.class),
                eq(correlationId),
                isNull()
        )).thenReturn(
                CompletableFuture.completedFuture(
                        new PublishOrderCreatedResult(eventId)
                )
        );

        CreateOrderResult result = service.createOrder(command).join();

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        verify(eventPublisher).publish(
                orderCaptor.capture(),
                eq(correlationId),
                isNull()
        );

        Order order = orderCaptor.getValue();

        assertThat(order.orderId()).isEqualTo("ORD-1001");
        assertThat(order.customerId()).isEqualTo("CUS-501");
        assertThat(order.currency()).isEqualTo("EUR");
        assertThat(order.salesChannel()).isEqualTo(SalesChannel.WEB);
        assertThat(order.items()).hasSize(2);
        assertThat(order.totalAmount()).isEqualByComparingTo("50.30");

        assertThat(result.orderId()).isEqualTo("ORD-1001");
        assertThat(result.eventId()).isEqualTo(eventId);
        assertThat(result.totalAmount()).isEqualByComparingTo("50.30");
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        UUID eventId = UUID.randomUUID();

        when(eventPublisher.publish(
                any(Order.class),
                any(UUID.class),
                isNull()
        )).thenReturn(
                CompletableFuture.completedFuture(
                        new PublishOrderCreatedResult(eventId)
                )
        );

        service.createOrder(createCommand(null, null)).join();

        verify(eventPublisher).publish(
                any(Order.class),
                any(UUID.class),
                isNull()
        );
    }

    @Test
    void shouldPreserveCausationId() {
        UUID correlationId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();

        when(eventPublisher.publish(
                any(Order.class),
                eq(correlationId),
                eq(causationId)
        )).thenReturn(
                CompletableFuture.completedFuture(
                        new PublishOrderCreatedResult(UUID.randomUUID())
                )
        );

        service.createOrder(
                createCommand(correlationId, causationId)
        ).join();

        verify(eventPublisher).publish(
                any(Order.class),
                eq(correlationId),
                eq(causationId)
        );
    }

    @Test
    void shouldPropagatePublicationFailure() {
        RuntimeException publicationFailure =
                new RuntimeException("Kafka unavailable");

        when(eventPublisher.publish(
                any(Order.class),
                any(UUID.class),
                isNull()
        )).thenReturn(
                CompletableFuture.failedFuture(publicationFailure)
        );

        CompletableFuture<CreateOrderResult> future =
                service.createOrder(
                        createCommand(UUID.randomUUID(), null)
                );

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCause(publicationFailure);
    }

    @Test
    void shouldCompleteOnlyAfterPublisherCompletes() {
        UUID eventId = UUID.randomUUID();
        CompletableFuture<PublishOrderCreatedResult> publicationFuture =
                new CompletableFuture<>();

        when(eventPublisher.publish(
                any(Order.class),
                any(UUID.class),
                isNull()
        )).thenReturn(publicationFuture);

        CompletableFuture<CreateOrderResult> resultFuture =
                service.createOrder(
                        createCommand(UUID.randomUUID(), null)
                );

        assertThat(resultFuture).isNotDone();

        publicationFuture.complete(
                new PublishOrderCreatedResult(eventId)
        );

        assertThat(resultFuture).isCompleted();
        assertThat(resultFuture.join().eventId()).isEqualTo(eventId);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.createOrder(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(eventPublisher);
    }

    private static CreateOrderCommand createCommand(
            UUID correlationId,
            UUID causationId
    ) {
        return new CreateOrderCommand(
                "ORD-1001",
                "CUS-501",
                "EUR",
                SalesChannel.WEB,
                List.of(
                        new OrderItem(
                                "PROD-101",
                                2,
                                new BigDecimal("19.90")
                        ),
                        new OrderItem(
                                "PROD-202",
                                1,
                                new BigDecimal("10.50")
                        )
                ),
                correlationId,
                causationId
        );
    }
}
