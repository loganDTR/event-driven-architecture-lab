package it.zengfx.order.application.service;

import it.zengfx.order.application.exception.EventContractViolationException;
import it.zengfx.order.application.port.in.CreateOrderCommand;
import it.zengfx.order.application.port.in.CreateOrderResult;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.application.port.out.ValidateOrderCreatedPort;
import it.zengfx.order.domain.event.OrderCreatedEvent;
import it.zengfx.order.domain.event.OrderItem;
import it.zengfx.order.domain.event.SalesChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.doThrow;

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

    @Mock
    private ValidateOrderCreatedPort eventValidator;

    private CreateOrderService service;

    @BeforeEach
    void setUp() {
        service = new CreateOrderService(
                eventPublisher,
                eventValidator
        );
    }

    @Test
    void shouldCreateAndPublishOrderCreatedEvent() {
        UUID correlationId = UUID.randomUUID();

        CreateOrderCommand command = createCommand(
                correlationId,
                null
        );

        when(eventPublisher.publish(any(OrderCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        CreateOrderResult result = service
                .createOrder(command)
                .join();

        assertThat(result.orderId()).isEqualTo("ORD-1001");
        assertThat(result.eventId()).isNotNull();
        assertThat(result.totalAmount())
                .isEqualByComparingTo("50.30");

        ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);

        verify(eventPublisher, times(1))
                .publish(eventCaptor.capture());

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();

        assertThat(publishedEvent.eventId())
                .isEqualTo(result.eventId());

        assertThat(publishedEvent.eventType())
                .isEqualTo("order.created");

        assertThat(publishedEvent.eventVersion())
                .isEqualTo(2);

        assertThat(publishedEvent.producer())
                .isEqualTo("java-order-service");

        assertThat(publishedEvent.correlationId())
                .isEqualTo(correlationId);

        assertThat(publishedEvent.causationId())
                .isNull();

        assertThat(publishedEvent.aggregateId())
                .isEqualTo("ORD-1001");

        assertThat(publishedEvent.payload().orderId())
                .isEqualTo("ORD-1001");

        assertThat(publishedEvent.payload().customerId())
                .isEqualTo("CUS-501");

        assertThat(publishedEvent.payload().currency())
                .isEqualTo("EUR");

        assertThat(publishedEvent.payload().salesChannel())
                .isEqualTo(SalesChannel.WEB);

        assertThat(publishedEvent.payload().totalAmount())
                .isEqualByComparingTo("50.30");

        assertThat(publishedEvent.payload().items())
                .hasSize(2);

        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        CreateOrderCommand command = createCommand(
                null,
                null
        );

        when(eventPublisher.publish(any(OrderCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.createOrder(command).join();

        ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventValidator)
                .validate(any(OrderCreatedEvent.class));
        verify(eventPublisher).publish(eventCaptor.capture());

        assertThat(eventCaptor.getValue().correlationId())
                .isNotNull();
    }

    @Test
    void shouldPreserveCausationId() {
        UUID correlationId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();

        CreateOrderCommand command = createCommand(
                correlationId,
                causationId
        );

        when(eventPublisher.publish(any(OrderCreatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.createOrder(command).join();

        ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventValidator)
                .validate(any(OrderCreatedEvent.class));
        verify(eventPublisher).publish(eventCaptor.capture());

        OrderCreatedEvent event = eventCaptor.getValue();

        assertThat(event.correlationId())
                .isEqualTo(correlationId);

        assertThat(event.causationId())
                .isEqualTo(causationId);
    }

    @Test
    void shouldPropagatePublicationFailure() {
        CreateOrderCommand command = createCommand(
                UUID.randomUUID(),
                null
        );

        RuntimeException publicationFailure =
                new RuntimeException("Kafka unavailable");

        when(eventPublisher.publish(any(OrderCreatedEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        publicationFailure
                ));

        CompletableFuture<CreateOrderResult> future =
                service.createOrder(command);

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCause(publicationFailure);
        verify(eventValidator)
                .validate(any(OrderCreatedEvent.class));
        verify(eventPublisher).publish(any(OrderCreatedEvent.class));
    }

    @Test
    void shouldNotPublishWhenContractValidationFails() {
        CreateOrderCommand command = createCommand(
                UUID.randomUUID(),
                null
        );

        EventContractViolationException failure =
                new EventContractViolationException(
                        "OrderCreated event violates JSON Schema V2",
                        List.of("/payload/customerId: required property missing")
                );

        doThrow(failure)
                .when(eventValidator)
                .validate(any(OrderCreatedEvent.class));

        assertThatThrownBy(() -> service.createOrder(command))
                .isSameAs(failure);

        verify(eventValidator)
                .validate(any(OrderCreatedEvent.class));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldCompleteOnlyAfterPublisherCompletes() {
        CreateOrderCommand command = createCommand(
                UUID.randomUUID(),
                null
        );

        CompletableFuture<Void> publicationFuture =
                new CompletableFuture<>();

        when(eventPublisher.publish(any(OrderCreatedEvent.class)))
                .thenReturn(publicationFuture);

        CompletableFuture<CreateOrderResult> resultFuture =
                service.createOrder(command);

        assertThat(resultFuture).isNotDone();

        publicationFuture.complete(null);

        assertThat(resultFuture).isCompleted();

        CreateOrderResult result = resultFuture.join();

        assertThat(result.orderId()).isEqualTo("ORD-1001");
        assertThat(result.totalAmount())
                .isEqualByComparingTo("50.30");
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.createOrder(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(eventPublisher);
    }

    private CreateOrderCommand createCommand(
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