package com.docflow.audit.infrastructure.adapters;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Health Controller for Audit Log Service.
 * <p>
 * Provides health check endpoints to verify service availability.
 * </p>
 */
@RestController
@Tag(name = "Health", description = "Health check endpoints for service verification")
public class HealthController {

    /**
     * Health check endpoint.
     *
     * @return A Mono containing the health status
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Health Check",
            description = "Returns the health status of the Audit Log Service"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"status\": \"ok\"}"
                            )
                    )
            )
    })
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "ok"));
    }

}
