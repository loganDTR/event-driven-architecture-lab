package it.zengfx.order.adapter.out.kafka;

import it.zengfx.order.adapter.out.kafka.message.OrderCreatedMessage;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.domain.model.Order;
import it.zengfx.order.domain.model.OrderItem;
import it.zengfx.order.domain.model.SalesChannel;
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
    void shouldPublishOrderCreatedMessageWithExpectedKeyAndPayload() {
        UUID correlationId = UUID.randomUUID();

        var publication = publisher.publish(
                order("ORD-IT-1001", "CUS-501", "50.30"),
                correlationId,
                null
        ).join();

        ConsumerRecord<String, OrderCreatedMessage> record =
                consumeSingleRecord();

        assertThat(record.topic()).isEqualTo(TOPIC);
        assertThat(record.key()).isEqualTo("ORD-IT-1001");

        OrderCreatedMessage message = record.value();

        assertThat(message.eventId()).isEqualTo(publication.eventId());
        assertThat(message.eventType()).isEqualTo("order.created");
        assertThat(message.eventVersion()).isEqualTo(2);
        assertThat(message.producer()).isEqualTo("java-order-service");
        assertThat(message.correlationId()).isEqualTo(correlationId);
        assertThat(message.aggregateId()).isEqualTo("ORD-IT-1001");
        assertThat(message.payload().orderId()).isEqualTo("ORD-IT-1001");
        assertThat(message.payload().totalAmount())
                .isEqualByComparingTo("50.30");
        assertThat(message.payload().items()).hasSize(1);
    }

    private ConsumerRecord<String, OrderCreatedMessage> consumeSingleRecord() {
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

        JacksonJsonDeserializer<OrderCreatedMessage> valueDeserializer =
                new JacksonJsonDeserializer<>(OrderCreatedMessage.class);

        valueDeserializer.setUseTypeHeaders(false);
        valueDeserializer.addTrustedPackages(
                "it.zengfx.order.adapter.out.kafka.message"
        );

        try (KafkaConsumer<String, OrderCreatedMessage> consumer =
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

    private static Order order(
            String orderId,
            String customerId,
            String amount
    ) {
        BigDecimal total = new BigDecimal(amount);

        return new Order(
                orderId,
                customerId,
                "EUR",
                SalesChannel.WEB,
                List.of(new OrderItem("PROD-TEST", 1, total))
        );
    }
}
