package com.docflow.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;

/**
 * Audit Log Service Application - Reactive microservice for audit event management.
 * <p>
 * This is the main entry point for the Audit Log microservice in DocFlow.
 * MongoDB auto-configuration is excluded until database setup is completed.
 * </p>
 *
 * @author DocFlow Team
 * @version 0.0.1-SNAPSHOT
 */
@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoReactiveAutoConfiguration.class,
        MongoReactiveDataAutoConfiguration.class
})
public class AuditLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditLogApplication.class, args);
    }

}
