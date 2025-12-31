package com.docflow.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Message Broker Service Application - Kafka Integration for DocFlow.
 * <p>
 * This is the main entry point for the Broker microservice.
 * Provides REST endpoints for publishing messages to Kafka topics
 * and includes a demo consumer for testing purposes.
 * </p>
 * <p>
 * DataSource auto-configuration is excluded as this service
 * does not require database connectivity.
 * </p>
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class BrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerApplication.class, args);
    }
}
