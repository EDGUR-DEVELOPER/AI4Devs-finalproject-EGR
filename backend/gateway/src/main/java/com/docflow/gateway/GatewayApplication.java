package com.docflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DocFlow Gateway Application - API Gateway for DocFlow microservices.
 * <p>
 * This is the main entry point for the Gateway microservice.
 * It routes requests to the appropriate backend services.
 * </p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    } 
}
