package com.docflow.documentcore;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Hello Controller - Health check and service verification endpoint.
 * <p>
 * This controller provides a simple endpoint to verify the service is running.
 * </p>
 */
@RestController
@Tag(name = "Hello", description = "Health check and service verification endpoints")
public class HelloController {

    /**
     * Simple hello endpoint to verify the service is running.
     *
     * @return a greeting message with service information
     */
    @GetMapping("/hello")
    @Operation(
            summary = "Hello Document Core",
            description = "Returns a greeting message to verify the Document Core Service is running correctly"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is running",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            )
    })
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of("message", "Hello Document Core"));
    }

}
