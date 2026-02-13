package com.docflow.documentcore.infrastructure.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del cliente MinIO para almacenamiento de objetos.
 * 
 * US-DOC-001: Configura la conexión a MinIO (S3-compatible) para producción.
 * Solo se carga cuando docflow.storage.type=minio
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "docflow.storage.type", havingValue = "minio", matchIfMissing = false)
public class MinioConfig {
    
    @Value("${docflow.storage.minio.endpoint:http://localhost:9000}")
    private String endpoint;
    
    @Value("${docflow.storage.minio.access-key:minioadmin}")
    private String accessKey;
    
    @Value("${docflow.storage.minio.secret-key:minioadmin123}")
    private String secretKey;
    
    @Value("${docflow.storage.minio.bucket-name:docflow-documents}")
    private String bucketName;
    
    /**
     * Crea y configura el cliente MinIO.
     * 
     * @return Cliente MinIO inicializado
     */
    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            
            // Verificar conexión y crear bucket si no existe
            initializeBucket(client);
            
            log.info("MinIO client initialized successfully at: {}", endpoint);
            return client;
            
        } catch (Exception e) {
            log.error("Error initializing MinIO client", e);
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }
    
    /**
     * Inicializa el bucket de MinIO si no existe.
     * 
     * @param client Cliente MinIO
     */
    private void initializeBucket(MinioClient client) {
        try {
            boolean exists = client.bucketExists(
                io.minio.BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            
            if (!exists) {
                client.makeBucket(
                    io.minio.MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                log.info("Bucket created: {}", bucketName);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO bucket: " + bucketName, e);
        }
    }
}
