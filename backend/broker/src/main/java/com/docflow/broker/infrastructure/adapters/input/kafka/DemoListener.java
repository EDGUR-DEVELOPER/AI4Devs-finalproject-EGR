package com.docflow.broker.infrastructure.adapters.input.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Demo Kafka listener for consuming messages from a configurable topic.
 * <p>
 * This listener demonstrates how to consume messages from Kafka topics.
 * The topic name is configurable via the 'broker.demo-topic' property.
 * </p>
 */
@Slf4j
@Component
public class DemoListener {

    @KafkaListener(
            topics = "${broker.demo-topic:demo-topic}",
            groupId = "${broker.consumer.group-id:broker-demo-group}"
    )
    public void listen(ConsumerRecord<String, String> record) {
        log.info("======================================");
        log.info("Received message from Kafka:");
        log.info("  Topic: {}", record.topic());
        log.info("  Partition: {}", record.partition());
        log.info("  Offset: {}", record.offset());
        log.info("  Key: {}", record.key());
        log.info("  Value: {}", record.value());
        log.info("  Timestamp: {}", record.timestamp());
        log.info("======================================");
    }
}
