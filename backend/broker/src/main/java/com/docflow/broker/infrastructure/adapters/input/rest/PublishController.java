package com.docflow.broker.infrastructure.adapters.input.rest;

import com.docflow.broker.application.dto.PublishRequest;
import com.docflow.broker.application.dto.PublishResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for publishing messages to Kafka topics.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Publish", description = "Endpoints for publishing messages to Kafka topics")
public class PublishController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping(value = "/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Publish Message",
            description = "Publishes a message to the specified Kafka topic and returns metadata about the operation"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Message published successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PublishResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Failed to publish message to Kafka",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public ResponseEntity<PublishResponse> publish(@Valid @RequestBody PublishRequest request) {
        log.info("Publishing message to topic: {}", request.getTopic());

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                request.getTopic(),
                request.getMessage()
        );

        try {
            SendResult<String, String> result = future.get();
            RecordMetadata metadata = result.getRecordMetadata();

            PublishResponse response = PublishResponse.builder()
                    .status("sent")
                    .topic(metadata.topic())
                    .partition(metadata.partition())
                    .offset(metadata.offset())
                    .timestamp(Instant.ofEpochMilli(metadata.timestamp()))
                    .build();

            log.info("Message published successfully to topic: {}, partition: {}, offset: {}",
                    metadata.topic(), metadata.partition(), metadata.offset());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to publish message to topic: {}", request.getTopic(), e);
            throw new RuntimeException("Failed to publish message: " + e.getMessage(), e);
        }
    }
}
