package it.zengfx.order.javaorderconsumer.adapter.in.kafka;

import it.zengfx.order.javaorderconsumer.adapter.in.kafka.message.OrderCreatedMessage;
import it.zengfx.order.javaorderconsumer.application.port.in.HandleOrderCreatedUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Component
public class OrderCreatedKafkaListener {

    private final ObjectMapper objectMapper;
    private final OrderCreatedMessageMapper mapper;
    private final HandleOrderCreatedUseCase useCase;

    public OrderCreatedKafkaListener(
            ObjectMapper objectMapper,
            OrderCreatedMessageMapper mapper,
            HandleOrderCreatedUseCase useCase
    ) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.useCase = useCase;
    }

    @KafkaListener(topics = "${event-lab.topics.order-created}")
    public void onMessage(String payload) {
        Objects.requireNonNull(payload, "payload cannot be null");

        OrderCreatedMessage message;
        try {
            message = objectMapper.readValue(payload, OrderCreatedMessage.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to deserialize OrderCreated event", exception);
        }

        useCase.handle(mapper.toDomain(message));
    }
}
