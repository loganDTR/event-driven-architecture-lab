package it.zengfx.order.adapter.out.kafka;

import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.domain.event.OrderCreatedEvent;
import it.zengfx.order.domain.event.OrderCreatedPayload;
import it.zengfx.order.domain.event.OrderItem;
import it.zengfx.order.domain.event.SalesChannel;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "event-lab.topics.order-created=orders.created.test"
})
class KafkaOrderCreatedPublisherIntegrationTest {

    private static final String TOPIC = "orders.created.test";

    @Container
    @ServiceConnection
    static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka-native:4.2.0")
            );

    @Autowired
    private PublishOrderCreatedPort publisher;

    @BeforeAll
    static void createTopic() throws Exception {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                "bootstrap.servers",
                kafka.getBootstrapServers()
        ))) {
            adminClient.createTopics(List.of(
                    new NewTopic(TOPIC, 3, (short) 1)
            )).all().get();
        }
    }

    @Test
    void shouldPublishOrderCreatedEventWithExpectedKeyAndPayload() {
        UUID correlationId = UUID.randomUUID();

        OrderCreatedPayload payload = new OrderCreatedPayload(
                "ORD-IT-1001",
                "CUS-501",
                "EUR",
                new BigDecimal("50.30"),
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
                )
        );

        OrderCreatedEvent event = OrderCreatedEvent.create(
                correlationId,
                null,
                payload
        );

        publisher.publish(event).join();

        ConsumerRecord<String, OrderCreatedEvent> record =
                consumeSingleRecord();

        assertThat(record.topic()).isEqualTo(TOPIC);
        assertThat(record.key()).isEqualTo("ORD-IT-1001");

        OrderCreatedEvent consumedEvent = record.value();

        assertThat(consumedEvent.eventId())
                .isEqualTo(event.eventId());

        assertThat(consumedEvent.eventType())
                .isEqualTo("order.created");

        assertThat(consumedEvent.eventVersion())
                .isEqualTo(2);

        assertThat(consumedEvent.producer())
                .isEqualTo("java-order-service");

        assertThat(consumedEvent.correlationId())
                .isEqualTo(correlationId);

        assertThat(consumedEvent.aggregateId())
                .isEqualTo("ORD-IT-1001");

        assertThat(consumedEvent.payload().orderId())
                .isEqualTo("ORD-IT-1001");

        assertThat(consumedEvent.payload().totalAmount())
                .isEqualByComparingTo("50.30");

        assertThat(consumedEvent.payload().items())
                .hasSize(2);
    }

    private ConsumerRecord<String, OrderCreatedEvent> consumeSingleRecord() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-created-integration-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        JacksonJsonDeserializer<OrderCreatedEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(OrderCreatedEvent.class);

        valueDeserializer.setUseTypeHeaders(false);
        valueDeserializer.addTrustedPackages(
                "it.zengfx.order.domain.event"
        );

        try (KafkaConsumer<String, OrderCreatedEvent> consumer =
                     new KafkaConsumer<>(
                             properties,
                             new StringDeserializer(),
                             valueDeserializer
                     )) {

            consumer.subscribe(List.of(TOPIC));

            long deadline = System.nanoTime()
                    + Duration.ofSeconds(10).toNanos();

            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));

                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }

        throw new AssertionError(
                "No record received from topic " + TOPIC
        );
    }
}