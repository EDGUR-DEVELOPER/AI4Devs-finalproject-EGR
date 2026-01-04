package com.docflow.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Identity Service Application - IAM Lightweight Keycloak Wrapper.
 * <p>
 * This is the main entry point for the Identity microservice.
 * Implements multi-organization authentication with JWT tokens.
 * </p>
 */
@SpringBootApplication
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }

}
