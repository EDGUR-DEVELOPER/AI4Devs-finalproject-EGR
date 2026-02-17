package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.exception.StorageException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Implementación de almacenamiento en MinIO (S3-compatible).
 * 
 * US-DOC-001: Almacenamiento para producción y entornos con infraestructura.
 * Activo cuando docflow.storage.type=minio o cuando no está configurado (valor por defecto)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "docflow.storage.type", havingValue = "minio", matchIfMissing = true)
public class MinioStorageService implements StorageService {
    
    private final MinioClient minioClient;
    private final String bucketName;
    
    public MinioStorageService(
        MinioClient minioClient,
        @Value("${docflow.storage.minio.bucket-name:docflow-documents}") String bucketName
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }
    
    @Override
    public String upload(Long organizacionId, Long carpetaId, Long documentoId, 
                        Integer numeroVersion, InputStream content, long contentLength) 
        throws StorageException {
        
        try {
            // Construir ruta: org_123/carpeta_456/doc_789/version_1/file
            String objectName = buildObjectName(organizacionId, carpetaId, documentoId, numeroVersion);
            
            // Subir archivo a MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(content, contentLength, -1)
                    .contentType("application/octet-stream")
                    .build()
            );
            
            log.info("Archivo guardado exitosamente en MinIO: {} ({} bytes)", objectName, contentLength);
            return objectName;
            
        } catch (Exception e) {
            throw new StorageException("Error al guardar archivo en MinIO", e);
        }
    }
    
    @Override
    public InputStream download(String storagePath) throws StorageException {
        try {
            // Validar que el objeto existe
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build()
            );
            
            // Descargar archivo desde MinIO
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build()
            );
            
            log.debug("Archivo descargado desde MinIO: {}", storagePath);
            return stream;
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().contentEquals("NoSuchKey")) {
                throw new StorageException("Archivo no encontrado: " + storagePath);
            }
            throw new StorageException("Error al descargar archivo de MinIO", e);
        } catch (Exception e) {
            throw new StorageException("Error al leer archivo de MinIO", e);
        }
    }
    
    @Override
    public void delete(String storagePath) throws StorageException {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build()
            );
            
            log.info("Archivo eliminado de MinIO: {}", storagePath);
            
        } catch (Exception e) {
            throw new StorageException("Error al eliminar archivo de MinIO", e);
        }
    }
    
    @Override
    public boolean exists(String storagePath) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().contentEquals("NoSuchKey")) {
                return false;
            }
            log.warn("Error checking file existence in MinIO: {}", storagePath, e);
            return false;
        } catch (Exception e) {
            log.warn("Error checking file existence in MinIO", e);
            return false;
        }
    }
    
    /**
     * Construye el nombre del objeto en MinIO.
     * Formato: org_{id}/carpeta_{id}/doc_{id}/version_{num}/file
     */
    private String buildObjectName(Long organizacionId, Long carpetaId, Long documentoId, Integer numeroVersion) {
        StringBuilder objectName = new StringBuilder();
        objectName.append("org_").append(organizacionId).append("/");
        
        if (carpetaId != null) {
            objectName.append("carpeta_").append(carpetaId).append("/");
        } else {
            objectName.append("sin_carpeta/");
        }
        
        objectName.append("doc_").append(documentoId).append("/");
        objectName.append("version_").append(numeroVersion).append("/");
        objectName.append("file");
        
        return objectName.toString();
    }
}
