package com.docflow.broker.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for publishing messages to Kafka topics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for publishing a message to a Kafka topic")
public class PublishRequest {

    @NotBlank(message = "Topic is required")
    @Size(min = 1, max = 255, message = "Topic must be between 1 and 255 characters")
    @Schema(
            description = "The Kafka topic to publish the message to",
            example = "demo-topic",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String topic;

    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 10000, message = "Message must be between 1 and 10000 characters")
    @Schema(
            description = "The message content to publish",
            example = "Hello from DocFlow!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;
}
