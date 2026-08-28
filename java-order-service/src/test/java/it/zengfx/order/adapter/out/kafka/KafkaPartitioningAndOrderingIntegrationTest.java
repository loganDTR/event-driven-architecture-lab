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
        OrderCreatedEvent firstEvent = createEvent(
                "ORD-1001",
                "CUS-501",
                "10.00"
        );

        OrderCreatedEvent secondEvent = createEvent(
                "ORD-1001",
                "CUS-501",
                "20.00"
        );

        OrderCreatedEvent differentOrderEvent = createEvent(
                "ORD-2001",
                "CUS-777",
                "30.00"
        );

        publisher.publish(firstEvent).join();
        publisher.publish(secondEvent).join();
        publisher.publish(differentOrderEvent).join();

        Set<UUID> expectedEventIds = Set.of(
                firstEvent.eventId(),
                secondEvent.eventId(),
                differentOrderEvent.eventId()
        );

        List<ConsumerRecord<String, OrderCreatedEvent>> records =
                consumeExpectedRecords(expectedEventIds);

        ConsumerRecord<String, OrderCreatedEvent> firstRecord =
                findByEventId(records, firstEvent.eventId());

        ConsumerRecord<String, OrderCreatedEvent> secondRecord =
                findByEventId(records, secondEvent.eventId());

        ConsumerRecord<String, OrderCreatedEvent> differentRecord =
                findByEventId(
                        records,
                        differentOrderEvent.eventId()
                );

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

    private List<ConsumerRecord<String, OrderCreatedEvent>>
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

        JacksonJsonDeserializer<OrderCreatedEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(OrderCreatedEvent.class);

        valueDeserializer.setUseTypeHeaders(false);
        valueDeserializer.addTrustedPackages(
                "it.zengfx.order.domain.event"
        );

        List<ConsumerRecord<String, OrderCreatedEvent>> received =
                new ArrayList<>();

        try (KafkaConsumer<String, OrderCreatedEvent> consumer =
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
            List<ConsumerRecord<String, OrderCreatedEvent>> records,
            Set<UUID> expectedEventIds
    ) {
        Set<UUID> receivedEventIds = records.stream()
                .map(record -> record.value().eventId())
                .collect(Collectors.toSet());

        return receivedEventIds.containsAll(expectedEventIds);
    }

    private ConsumerRecord<String, OrderCreatedEvent> findByEventId(
            List<ConsumerRecord<String, OrderCreatedEvent>> records,
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

    private OrderCreatedEvent createEvent(
            String orderId,
            String customerId,
            String totalAmount
    ) {
        BigDecimal amount = new BigDecimal(totalAmount);

        OrderCreatedPayload payload = new OrderCreatedPayload(
                orderId,
                customerId,
                "EUR",
                amount,
                SalesChannel.WEB,
                List.of(
                        new OrderItem(
                                "PROD-TEST",
                                1,
                                amount
                        )
                )
        );

        return OrderCreatedEvent.create(
                UUID.randomUUID(),
                null,
                payload
        );
    }
}