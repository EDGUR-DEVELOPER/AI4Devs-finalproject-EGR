package com.docflow.documentcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Document Core Service Application - Document Management for DocFlow.
 * <p>
 * This is the main entry point for the Document Core microservice.
 * </p>
 */
@SpringBootApplication()
public class DocumentCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentCoreApplication.class, args);
    }

}
