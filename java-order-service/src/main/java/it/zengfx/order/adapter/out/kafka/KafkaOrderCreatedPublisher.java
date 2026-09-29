package it.zengfx.order.adapter.out.kafka;

import it.zengfx.order.application.exception.EventPublicationException;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaOrderCreatedPublisher implements PublishOrderCreatedPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderCreatedPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaOrderCreatedPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                      @Value("${event-lab.topics.order-created}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }


    @Override
    public CompletableFuture<Void> publish(OrderCreatedEvent event) {
        String messageKey = event.aggregateId();

        return kafkaTemplate
                .send(topic, messageKey, event)
                .thenAccept(result -> log.info(
                        "Published event: eventId={}, topic={}, partition={}, offset={}, key={}",
                        event.eventId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        messageKey
                ))
                .exceptionally(error -> {
                    log.error("Failed to publish event: eventId={}, topic={}, key={}",
                            event.eventId(),
                            topic,
                            messageKey,
                            error);
                    throw new EventPublicationException(
                            "Unable to publish OrderCreated event " + event.eventId() + " to Kafka topic " + topic, error
                    );
                });
    }
}
