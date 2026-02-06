package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.exception.StorageException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Implementación de almacenamiento en sistema de archivos local.
 * 
 * US-DOC-001: Almacenamiento por defecto para desarrollo/MVP.
 * Para producción, reemplazar con adaptador S3/MinIO/Azure manteniendo la interfaz.
 */
@Slf4j
@Service
public class LocalStorageService implements StorageService {
    
    private final Path rootLocation;
    
    public LocalStorageService(
        @Value("${docflow.storage.local.path:./storage}") String storagePath
    ) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        initializeStorage();
    }
    
    private void initializeStorage() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Directorio de almacenamiento inicializado en: {}", rootLocation);
        } catch (IOException e) {
            throw new StorageException("No se pudo crear el directorio de almacenamiento", e);
        }
    }
    
    @Override
    public String upload(Long organizacionId, Long carpetaId, Long documentoId, 
                        Integer numeroVersion, InputStream content, long contentLength) 
        throws StorageException {
        
        try {
            // Construir ruta: /org_123/carpeta_456/doc_789/version_1/file
            String relativePath = buildPath(organizacionId, carpetaId, documentoId, numeroVersion);
            Path targetLocation = rootLocation.resolve(relativePath).normalize();
            
            // Validar que la ruta está dentro del directorio raíz (seguridad)
            if (!targetLocation.startsWith(rootLocation)) {
                throw new StorageException("Intento de path traversal detectado");
            }
            
            // Crear directorios padre si no existen
            Files.createDirectories(targetLocation.getParent());
            
            // Copiar archivo
            Files.copy(content, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Archivo guardado exitosamente en: {} ({} bytes)", relativePath, contentLength);
            return relativePath;
            
        } catch (IOException e) {
            throw new StorageException("Error al guardar archivo en almacenamiento local", e);
        }
    }
    
    @Override
    public InputStream download(String storagePath) throws StorageException {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            
            // Validar seguridad
            if (!filePath.startsWith(rootLocation)) {
                throw new StorageException("Intento de path traversal detectado");
            }
            
            if (!Files.exists(filePath)) {
                throw new StorageException("Archivo no encontrado: " + storagePath);
            }
            
            return Files.newInputStream(filePath);
            
        } catch (IOException e) {
            throw new StorageException("Error al leer archivo del almacenamiento", e);
        }
    }
    
    @Override
    public void delete(String storagePath) throws StorageException {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            
            // Validar seguridad
            if (!filePath.startsWith(rootLocation)) {
                throw new StorageException("Intento de path traversal detectado");
            }
            
            Files.deleteIfExists(filePath);
            log.info("Archivo eliminado: {}", storagePath);
            
        } catch (IOException e) {
            throw new StorageException("Error al eliminar archivo del almacenamiento", e);
        }
    }
    
    @Override
    public boolean exists(String storagePath) {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Construye la ruta relativa para almacenar el archivo.
     * Formato: org_{id}/carpeta_{id}/doc_{id}/version_{num}/file
     */
    private String buildPath(Long organizacionId, Long carpetaId, Long documentoId, Integer numeroVersion) {
        StringBuilder path = new StringBuilder();
        path.append("org_").append(organizacionId).append("/");
        
        if (carpetaId != null) {
            path.append("carpeta_").append(carpetaId).append("/");
        } else {
            path.append("sin_carpeta/");
        }
        
        path.append("doc_").append(documentoId).append("/");
        path.append("version_").append(numeroVersion).append("/");
        path.append("file");
        
        return path.toString();
    }
}
