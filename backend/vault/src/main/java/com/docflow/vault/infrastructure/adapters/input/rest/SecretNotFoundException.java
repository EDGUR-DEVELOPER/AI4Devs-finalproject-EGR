package com.docflow.vault.infrastructure.adapters.input.rest;

/**
 * Exception thrown when a secret is not found in Vault.
 */
public class SecretNotFoundException extends RuntimeException {

    private final String path;

    public SecretNotFoundException(String path) {
        super("Secret not found at path: " + path);
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
