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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "event-lab.topics.order-created=orders.created.partitioning.test"
})
class KafkaPartitioningAndOrderingIntegrationTest {

    private static final String TOPIC =
            "orders.created.partitioning.test";

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
    void shouldUseSamePartitionAndPreserveOrderForSameAggregateId() {
        UUID correlationId = UUID.randomUUID();

        var firstPublication = publisher.publish(
                order("ORD-1001", "CUS-501", "10.00"),
                correlationId,
                null
        ).join();

        var secondPublication = publisher.publish(
                order("ORD-1001", "CUS-501", "20.00"),
                correlationId,
                null
        ).join();

        var differentPublication = publisher.publish(
                order("ORD-2001", "CUS-777", "30.00"),
                correlationId,
                null
        ).join();

        Set<UUID> expectedEventIds = Set.of(
                firstPublication.eventId(),
                secondPublication.eventId(),
                differentPublication.eventId()
        );

        List<ConsumerRecord<String, OrderCreatedMessage>> records =
                consumeExpectedRecords(expectedEventIds);

        ConsumerRecord<String, OrderCreatedMessage> firstRecord =
                findByEventId(records, firstPublication.eventId());

        ConsumerRecord<String, OrderCreatedMessage> secondRecord =
                findByEventId(records, secondPublication.eventId());

        ConsumerRecord<String, OrderCreatedMessage> differentRecord =
                findByEventId(records, differentPublication.eventId());

        assertThat(firstRecord.key()).isEqualTo("ORD-1001");
        assertThat(secondRecord.key()).isEqualTo("ORD-1001");
        assertThat(differentRecord.key()).isEqualTo("ORD-2001");

        assertThat(firstRecord.partition())
                .as("Events with the same key must use the same partition")
                .isEqualTo(secondRecord.partition());

        assertThat(firstRecord.offset())
                .as("The first event must precede the second one")
                .isLessThan(secondRecord.offset());
    }

    private List<ConsumerRecord<String, OrderCreatedMessage>>
    consumeExpectedRecords(Set<UUID> expectedEventIds) {

        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "partition-order-test-" + UUID.randomUUID(),
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

        List<ConsumerRecord<String, OrderCreatedMessage>> received =
                new ArrayList<>();

        try (KafkaConsumer<String, OrderCreatedMessage> consumer =
                     new KafkaConsumer<>(
                             properties,
                             new StringDeserializer(),
                             valueDeserializer
                     )) {

            consumer.subscribe(List.of(TOPIC));

            long deadline = System.nanoTime()
                    + Duration.ofSeconds(10).toNanos();

            while (
                    !containsAllExpectedEvents(received, expectedEventIds)
                            && System.nanoTime() < deadline
            ) {
                consumer.poll(Duration.ofMillis(500))
                        .forEach(record -> {
                            if (expectedEventIds.contains(
                                    record.value().eventId()
                            )) {
                                received.add(record);
                            }
                        });
            }
        }

        assertThat(received)
                .extracting(record -> record.value().eventId())
                .containsExactlyInAnyOrderElementsOf(expectedEventIds);

        return received;
    }

    private boolean containsAllExpectedEvents(
            List<ConsumerRecord<String, OrderCreatedMessage>> records,
            Set<UUID> expectedEventIds
    ) {
        Set<UUID> receivedEventIds = records.stream()
                .map(record -> record.value().eventId())
                .collect(Collectors.toSet());

        return receivedEventIds.containsAll(expectedEventIds);
    }

    private ConsumerRecord<String, OrderCreatedMessage> findByEventId(
            List<ConsumerRecord<String, OrderCreatedMessage>> records,
            UUID eventId
    ) {
        return records.stream()
                .filter(record ->
                        record.value().eventId().equals(eventId)
                )
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Record not found for eventId " + eventId
                ));
    }

    private static Order order(
            String orderId,
            String customerId,
            String totalAmount
    ) {
        BigDecimal amount = new BigDecimal(totalAmount);

        return new Order(
                orderId,
                customerId,
                "EUR",
                SalesChannel.WEB,
                List.of(
                        new OrderItem(
                                "PROD-TEST",
                                1,
                                amount
                        )
                )
        );
    }
}
