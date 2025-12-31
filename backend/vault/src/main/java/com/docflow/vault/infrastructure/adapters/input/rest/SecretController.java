package com.docflow.vault.infrastructure.adapters.input.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for managing secrets from HashiCorp Vault.
 * <p>
 * Provides endpoints for reading secrets from the configured Vault instance.
 * </p>
 */
@RestController
@RequestMapping("/secret")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Secrets", description = "Endpoints for reading secrets from HashiCorp Vault")
public class SecretController {

    private final VaultTemplate vaultTemplate;

    /**
     * Reads a secret from Vault at the specified path.
     *
     * @param path the path to the secret in Vault
     * @return JSON response containing the secret data
     */
    @GetMapping(value = "/{path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Read Secret",
            description = "Reads a secret from HashiCorp Vault at the specified path"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Secret retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"data\": \"secret-value\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Secret not found at the specified path",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"error\": \"Secret not found\", \"path\": \"my-secret\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error while accessing Vault",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"error\": \"Internal server error\", \"message\": \"Error details\"}"
                            )
                    )
            )
    })
    public ResponseEntity<Map<String, Object>> getSecret(
            @Parameter(description = "The path to the secret in Vault", required = true, example = "my-secret")
            @PathVariable String path) {

        log.debug("Reading secret from path: {}", path);

        VaultKeyValueOperations kvOps = vaultTemplate.opsForKeyValue(
                "secret",
                VaultKeyValueOperationsSupport.KeyValueBackend.KV_2
        );

        VaultResponse response = kvOps.get(path);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            log.warn("Secret not found at path: {}", path);
            throw new SecretNotFoundException(path);
        }

        // Extract the first value from the secret data
        Object secretValue = response.getData().values().iterator().next();
        log.debug("Secret retrieved successfully from path: {}", path);

        return ResponseEntity.ok(Map.of("data", secretValue));
    }
}
