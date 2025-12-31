package com.docflow.broker.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for publish operations containing metadata about the sent message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing metadata about the published message")
public class PublishResponse {

    @Schema(
            description = "Status of the publish operation",
            example = "sent"
    )
    private String status;

    @Schema(
            description = "The Kafka topic the message was published to",
            example = "demo-topic"
    )
    private String topic;

    @Schema(
            description = "The partition the message was sent to",
            example = "0"
    )
    private Integer partition;

    @Schema(
            description = "The offset of the message within the partition",
            example = "42"
    )
    private Long offset;

    @Schema(
            description = "Timestamp when the message was sent",
            example = "2025-12-31T10:30:00.123Z"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
}
