package com.docflow.documentcore.application.service;

import java.io.InputStream;

import com.docflow.documentcore.domain.exception.StorageException;

/**
 * Interfaz de servicio de almacenamiento (Puerto en Arquitectura Hexagonal).
 * 
 * US-DOC-001: Define operaciones abstractas de almacenamiento.
 * Permite múltiples implementaciones: local, S3, MinIO, Azure Blob, etc.
 */
public interface StorageService {
    
    /**
     * Sube un archivo al almacenamiento.
     *
     * @param organizacionId ID de la organización (para particionamiento)
     * @param carpetaId ID de la carpeta (puede ser null)
     * @param documentoId ID del documento
     * @param numeroVersion Número de versión (1, 2, 3, ...)
     * @param content Contenido del archivo como InputStream
     * @param contentLength Tamaño del archivo en bytes
     * @return Ruta de almacenamiento donde se guardó el archivo
     * @throws StorageException Si ocurre un error al guardar
     */
    String upload(Long organizacionId, Long carpetaId, Long documentoId, 
                  Integer numeroVersion, InputStream content, long contentLength) 
        throws StorageException;
    
    /**
     * Descarga un archivo del almacenamiento.
     *
     * @param storagePath Ruta del archivo en almacenamiento
     * @return InputStream con el contenido del archivo
     * @throws StorageException Si el archivo no existe o hay error al leer
     */
    InputStream download(String storagePath) throws StorageException;
    
    /**
     * Elimina un archivo del almacenamiento.
     *
     * @param storagePath Ruta del archivo en almacenamiento
     * @throws StorageException Si hay error al eliminar
     */
    void delete(String storagePath) throws StorageException;
    
    /**
     * Verifica si existe un archivo en almacenamiento.
     *
     * @param storagePath Ruta del archivo en almacenamiento
     * @return true si el archivo existe
     */
    boolean exists(String storagePath);
}
