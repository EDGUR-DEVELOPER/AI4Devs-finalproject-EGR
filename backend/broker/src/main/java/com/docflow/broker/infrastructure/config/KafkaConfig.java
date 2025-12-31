package com.docflow.broker.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for the Message Broker Service.
 * <p>
 * Configures the demo topic to be auto-created if it doesn't exist.
 * KafkaTemplate is auto-configured by Spring Boot based on application.yml settings.
 * </p>
 */
@Configuration
public class KafkaConfig {

    @Value("${broker.demo-topic:demo-topic}")
    private String demoTopic;

    /**
     * Creates the demo topic if it doesn't exist.
     * Configured with 1 partition and replication factor of 1 for local development.
     */
    @Bean
    public NewTopic demoTopic() {
        return TopicBuilder.name(demoTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
