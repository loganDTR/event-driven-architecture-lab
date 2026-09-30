package it.zengfx.order.adapter.out.kafka;

import it.zengfx.order.adapter.out.validation.JsonSchemaOrderCreatedValidator;
import it.zengfx.order.application.exception.EventPublicationException;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.application.port.out.PublishOrderCreatedResult;
import it.zengfx.order.domain.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class KafkaOrderCreatedPublisher implements PublishOrderCreatedPort {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaOrderCreatedPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderCreatedMessageMapper mapper;
    private final JsonSchemaOrderCreatedValidator validator;
    private final String topic;

    public KafkaOrderCreatedPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            OrderCreatedMessageMapper mapper,
            JsonSchemaOrderCreatedValidator validator,
            @Value("${event-lab.topics.order-created}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.validator = validator;
        this.topic = topic;
    }

    @Override
    public CompletableFuture<PublishOrderCreatedResult> publish(
            Order order,
            UUID correlationId,
            UUID causationId
    ) {
        var message = mapper.toMessage(order, correlationId, causationId);
        validator.validate(message);

        String messageKey = message.aggregateId();

        return kafkaTemplate
                .send(topic, messageKey, message)
                .thenApply(result -> {
                    log.info(
                            "Published event: eventId={}, topic={}, partition={}, offset={}, key={}",
                            message.eventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            messageKey
                    );

                    return new PublishOrderCreatedResult(message.eventId());
                })
                .exceptionally(error -> {
                    log.error(
                            "Failed to publish event: eventId={}, topic={}, key={}",
                            message.eventId(),
                            topic,
                            messageKey,
                            error
                    );

                    throw new EventPublicationException(
                            "Unable to publish OrderCreated event "
                                    + message.eventId()
                                    + " to Kafka topic "
                                    + topic,
                            error
                    );
                });
    }
}
