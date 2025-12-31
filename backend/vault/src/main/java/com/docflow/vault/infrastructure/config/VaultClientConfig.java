package com.docflow.vault.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

/**
 * Configuration class for HashiCorp Vault client.
 * <p>
 * Configures and exposes a VaultTemplate bean using properties from application.yml.
 * </p>
 */
@Configuration
public class VaultClientConfig {

    @Value("${spring.cloud.vault.uri}")
    private String vaultUri;

    @Value("${spring.cloud.vault.token}")
    private String vaultToken;

    @Value("${spring.cloud.vault.kv.backend}")
    private String kvBackend;

    @Value("${spring.cloud.vault.kv.default-context}")
    private String kvDefaultContext;

    /**
     * Creates and configures the VaultTemplate bean.
     *
     * @return configured VaultTemplate for interacting with HashiCorp Vault
     */
    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(vaultUri));
        return new VaultTemplate(endpoint, new TokenAuthentication(vaultToken));
    }

    /**
     * Returns the configured KV backend path.
     *
     * @return the KV secrets engine backend path
     */
    public String getKvBackend() {
        return kvBackend;
    }

    /**
     * Returns the configured default context for KV operations.
     *
     * @return the default context path
     */
    public String getKvDefaultContext() {
        return kvDefaultContext;
    }
}
