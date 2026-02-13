package com.docflow.documentcore.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.docflow.documentcore.application.dto.DownloadDocumentDto;
import com.docflow.documentcore.domain.event.DocumentDownloadedEvent;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.StorageException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import com.docflow.documentcore.infrastructure.security.SecurityContext;

/**
 * Tests unitarios para DocumentService - funcionalidad de descarga.
 * 
 * <p>Sigue enfoque TDD y valida:
 * <ul>
 *   <li>Caso exitoso con permiso LECTURA</li>
 *   <li>Casos exitosos con niveles de permiso superiores (ESCRITURA, ADMINISTRACION)</li>
 *   <li>Validaciones de existencia (documento, versión)</li>
 *   <li>Validaciones de permisos (LECTURA mínimo requerido)</li>
 *   <li>Tenant isolation (documentos de otras organizaciones)</li>
 *   <li>Errores de almacenamiento (archivo no disponible)</li>
 *   <li>Eventos de auditoría (DOCUMENTO_DESCARGADO)</li>
 * </ul>
 * 
 * <p><strong>US-DOC-002:</strong> Descarga de versión actual de documento.
 * 
 * @see DocumentService#downloadDocument(Long)
 */
@DisplayName("DocumentService - Download Document")
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    @Mock
    private DocumentoRepository documentoRepository;
    
    @Mock
    private VersionRepository versionRepository;
    
    @Mock
    private StorageService storageService;
    
    @Mock
    private IEvaluadorPermisos evaluadorPermisos;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PermisoDocumentoUsuarioService permisoDocumentoUsuarioService;
    
    @Mock
    private com.docflow.documentcore.application.validator.DocumentValidator documentValidator;
    
    private DocumentService service;
    
    private static final Long DOCUMENTO_ID = 100L;
    private static final Long VERSION_ID = 200L;
    private static final Long USUARIO_ID = 1L;
    private static final Long ORGANIZACION_ID = 1L;
    private static final String NOMBRE_ARCHIVO = "reporte-anual";
    private static final String EXTENSION = "pdf";
    private static final String TIPO_CONTENIDO = "application/pdf";
    private static final Long TAMANIO_BYTES = 1024000L;
    private static final String RUTA_ALMACENAMIENTO = "org_1/carpeta_10/doc_100/version_1/file";
    
    @BeforeEach
    void setUp() {
        // Crear servicio con todas las dependencias mockeadas
        service = new DocumentService(
            documentoRepository,
            versionRepository,
            storageService,
            documentValidator,
            null, // documentoMapper (no usado en tests actuales)
            securityContext,
            evaluadorPermisos,
            eventPublisher,
            permisoDocumentoUsuarioService
        );
        
        // Configurar SecurityContext por defecto
        when(securityContext.getUsuarioId()).thenReturn(USUARIO_ID);
        when(securityContext.getOrganizacionId()).thenReturn(ORGANIZACION_ID);
    }
    
    // ========================================================================
    // CASOS EXITOSOS
    // ========================================================================
    
    @Test
    @DisplayName("should_DownloadDocument_When_UserHasReadPermission")
    void shouldDownloadDocumentWhenUserHasReadPermission() {
        // Arrange
        Documento documento = crearDocumento();
        Version version = crearVersion();
        InputStream mockStream = new ByteArrayInputStream("test content".getBytes());
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true);
        when(versionRepository.findById(VERSION_ID))
            .thenReturn(Optional.of(version));
        when(storageService.download(RUTA_ALMACENAMIENTO))
            .thenReturn(mockStream);
        
        // Act
        DownloadDocumentDto result = service.downloadDocument(DOCUMENTO_ID);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.stream()).isEqualTo(mockStream);
        assertThat(result.filename()).isEqualTo(NOMBRE_ARCHIVO);
        assertThat(result.extension()).isEqualTo(EXTENSION);
        assertThat(result.mimeType()).isEqualTo(TIPO_CONTENIDO);
        assertThat(result.sizeBytes()).isEqualTo(TAMANIO_BYTES);
        
        // Verificar que se publicó evento de auditoría
        ArgumentCaptor<DocumentDownloadedEvent> eventCaptor = 
            ArgumentCaptor.forClass(DocumentDownloadedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        DocumentDownloadedEvent event = eventCaptor.getValue();
        assertThat(event.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
        assertThat(event.getVersionId()).isEqualTo(VERSION_ID);
        assertThat(event.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(event.getOrganizacionId()).isEqualTo(ORGANIZACION_ID);
        assertThat(event.getTamanioBytes()).isEqualTo(TAMANIO_BYTES);
    }
    
    @Test
    @DisplayName("should_DownloadDocument_When_UserHasWritePermission")
    void shouldDownloadDocumentWhenUserHasWritePermission() {
        // Arrange
        Documento documento = crearDocumento();
        Version version = crearVersion();
        InputStream mockStream = new ByteArrayInputStream("test content".getBytes());
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true); // ESCRITURA incluye LECTURA
        when(versionRepository.findById(VERSION_ID))
            .thenReturn(Optional.of(version));
        when(storageService.download(RUTA_ALMACENAMIENTO))
            .thenReturn(mockStream);
        
        // Act
        DownloadDocumentDto result = service.downloadDocument(DOCUMENTO_ID);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.stream()).isNotNull();
    }
    
    @Test
    @DisplayName("should_DownloadDocument_When_UserHasAdminPermission")
    void shouldDownloadDocumentWhenUserHasAdminPermission() {
        // Arrange
        Documento documento = crearDocumento();
        Version version = crearVersion();
        InputStream mockStream = new ByteArrayInputStream("test content".getBytes());
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true); // ADMINISTRACION incluye LECTURA
        when(versionRepository.findById(VERSION_ID))
            .thenReturn(Optional.of(version));
        when(storageService.download(RUTA_ALMACENAMIENTO))
            .thenReturn(mockStream);
        
        // Act
        DownloadDocumentDto result = service.downloadDocument(DOCUMENTO_ID);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.stream()).isNotNull();
    }
    
    // ========================================================================
    // ERRORES DE PERMISOS (403 FORBIDDEN)
    // ========================================================================
    
    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_UserHasNoPermission")
    void shouldThrowAccessDeniedExceptionWhenUserHasNoPermission() {
        // Arrange
        Documento documento = crearDocumento();
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> service.downloadDocument(DOCUMENTO_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("No tiene permisos de lectura sobre este documento");
        
        // Verificar que NO se intenta descargar archivo ni publicar evento
        verify(storageService, never()).download(anyString());
        verify(eventPublisher, never()).publishEvent(any());
    }
    
    // ========================================================================
    // ERRORES DE DOCUMENTO NO ENCONTRADO (404 NOT FOUND)
    // ========================================================================
    
    @Test
    @DisplayName("should_ThrowResourceNotFoundException_When_DocumentNotFound")
    void shouldThrowResourceNotFoundExceptionWhenDocumentNotFound() {
        // Arrange
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.downloadDocument(DOCUMENTO_ID))
            .isInstanceOf(ResourceNotFoundException.class);
        
        // Verificar que NO se verifican permisos
        verify(evaluadorPermisos, never()).tieneAcceso(
            anyLong(), anyLong(), any(), any(), anyLong());
    }
    
    @Test
    @DisplayName("should_ThrowResourceNotFoundException_When_DocumentBelongsToDifferentOrganization")
    void shouldThrowResourceNotFoundExceptionWhenDocumentBelongsToDifferentOrganization() {
        // Arrange - Simular tenant isolation: documento existe pero en otra organización
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.empty()); // El repositorio filtra por org_id
        
        // Act & Assert
        assertThatThrownBy(() -> service.downloadDocument(DOCUMENTO_ID))
            .isInstanceOf(ResourceNotFoundException.class);
        
        // Importante: 404 en lugar de 403 para no revelar existencia del documento
        // (security by obscurity per US-AUTH-004)
    }
    
    // ========================================================================
    // ERRORES DE ALMACENAMIENTO (500 INTERNAL SERVER ERROR)
    // ========================================================================
    
    @Test
    @DisplayName("should_ThrowStorageException_When_VersionHasNoCurrentVersion")
    void shouldThrowStorageExceptionWhenVersionHasNoCurrentVersion() {
        // Arrange
        Documento documento = crearDocumento();
        documento.setVersionActualId(null); // Sin versión actual
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> service.downloadDocument(DOCUMENTO_ID))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("no tiene una versión actual asignada");
    }
    
    @Test
    @DisplayName("should_ThrowStorageException_When_CurrentVersionNotFound")
    void shouldThrowStorageExceptionWhenCurrentVersionNotFound() {
        // Arrange
        Documento documento = crearDocumento();
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true);
        when(versionRepository.findById(VERSION_ID))
            .thenReturn(Optional.empty()); // Versión no encontrada
        
        // Act & Assert
        assertThatThrownBy(() -> service.downloadDocument(DOCUMENTO_ID))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("versión actual del documento no está disponible");
    }
    
    @Test
    @DisplayName("should_ThrowStorageException_When_FileNotFoundInStorage")
    void shouldThrowStorageExceptionWhenFileNotFoundInStorage() {
        // Arrange
        Documento documento = crearDocumento();
        Version version = crearVersion();
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true);
        when(versionRepository.findById(VERSION_ID))
            .thenReturn(Optional.of(version));
        when(storageService.download(RUTA_ALMACENAMIENTO))
            .thenThrow(new StorageException("Archivo no encontrado"));
        
        // Act & Assert
        assertThatThrownBy(() -> service.downloadDocument(DOCUMENTO_ID))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("archivo del documento no está disponible en el almacenamiento");
        
        // Verificar que NO se publica evento si falla la descarga
        verify(eventPublisher, never()).publishEvent(any());
    }
    
    // ========================================================================
    // EVENTOS DE AUDITORÍA
    // ========================================================================
    
    @Test
    @DisplayName("should_PublishAuditEvent_When_DownloadSuccessful")
    void shouldPublishAuditEventWhenDownloadSuccessful() {
        // Arrange
        Documento documento = crearDocumento();
        Version version = crearVersion();
        InputStream mockStream = new ByteArrayInputStream("test content".getBytes());
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true);
        when(versionRepository.findById(VERSION_ID))
            .thenReturn(Optional.of(version));
        when(storageService.download(RUTA_ALMACENAMIENTO))
            .thenReturn(mockStream);
        
        // Act
        service.downloadDocument(DOCUMENTO_ID);
        
        // Assert
        ArgumentCaptor<DocumentDownloadedEvent> eventCaptor = 
            ArgumentCaptor.forClass(DocumentDownloadedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        
        DocumentDownloadedEvent event = eventCaptor.getValue();
        assertThat(event).isNotNull();
        assertThat(event.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
        assertThat(event.getVersionId()).isEqualTo(VERSION_ID);
        assertThat(event.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(event.getOrganizacionId()).isEqualTo(ORGANIZACION_ID);
        assertThat(event.getTamanioBytes()).isEqualTo(TAMANIO_BYTES);
        assertThat(event.getEventTimestamp()).isNotNull();
    }
    
    @Test
    @DisplayName("should_NotFailDownload_When_EventPublishingFails")
    void shouldNotFailDownloadWhenEventPublishingFails() {
        // Arrange
        Documento documento = crearDocumento();
        Version version = crearVersion();
        InputStream mockStream = new ByteArrayInputStream("test content".getBytes());
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.LECTURA, ORGANIZACION_ID))
            .thenReturn(true);
        when(versionRepository.findById(VERSION_ID))
            .thenReturn(Optional.of(version));
        when(storageService.download(RUTA_ALMACENAMIENTO))
            .thenReturn(mockStream);
        doThrow(new RuntimeException("Event bus failure"))
            .when(eventPublisher).publishEvent(any());
        
        // Act & Assert - La descarga NO debe fallar si falla el evento
        assertThatCode(() -> service.downloadDocument(DOCUMENTO_ID))
            .doesNotThrowAnyException();
    }
    
    // ========================================================================
    // TESTS DE CREACIÓN DE VERSIÓN (US-DOC-003)
    // ========================================================================
    
    @Test
    @DisplayName("should_CreateNewVersion_When_UserHasWritePermission")
    void shouldCreateNewVersionWhenUserHasWritePermission() throws Exception {
        // Arrange
        Documento documento = crearDocumento();
        documento.setNumeroVersiones(1);
        documento.setVersionActualId(VERSION_ID);
        
        org.springframework.web.multipart.MultipartFile mockFile = 
            mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getBytes()).thenReturn("new content".getBytes());
        when(mockFile.getSize()).thenReturn(2048L);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("new content".getBytes()));
        
        com.docflow.documentcore.application.dto.CreateVersionRequest request = 
            new com.docflow.documentcore.application.dto.CreateVersionRequest(mockFile, "Actualización Q1 2026");
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.ESCRITURA, ORGANIZACION_ID))
            .thenReturn(true);
        when(storageService.upload(anyLong(), nullable(Long.class), anyLong(), anyInt(), any(), anyLong()))
            .thenReturn("org_1/carpeta_10/doc_100/version_2/file");
        
        Version savedVersion = new Version();
        savedVersion.setId(201L);
        savedVersion.setDocumentoId(DOCUMENTO_ID);
        savedVersion.setNumeroSecuencial(2);
        savedVersion.setTamanioBytes(2048L);
        savedVersion.setRutaAlmacenamiento("org_1/carpeta_10/doc_100/version_2/file");
        savedVersion.setHashContenido("newhash123");
        savedVersion.setComentarioCambio("Actualización Q1 2026");
        savedVersion.setCreadoPor(USUARIO_ID);
        savedVersion.setFechaCreacion(OffsetDateTime.now());
        
        when(versionRepository.save(any(Version.class))).thenReturn(savedVersion);
        when(documentoRepository.save(any(Documento.class))).thenReturn(documento);
        
        // Act
        com.docflow.documentcore.application.dto.VersionResponse result = 
            service.createVersion(DOCUMENTO_ID, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(201L);
        assertThat(result.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
        assertThat(result.getNumeroSecuencial()).isEqualTo(2);
        assertThat(result.getTamanioBytes()).isEqualTo(2048L);
        assertThat(result.getComentarioCambio()).isEqualTo("Actualización Q1 2026");
        assertThat(result.getEsVersionActual()).isTrue();
        
        // Verificar que se guardó la versión
        ArgumentCaptor<Version> versionCaptor = ArgumentCaptor.forClass(Version.class);
        verify(versionRepository).save(versionCaptor.capture());
        Version capturedVersion = versionCaptor.getValue();
        assertThat(capturedVersion.getNumeroSecuencial()).isEqualTo(2);
        
        // Verificar que se actualizó el documento
        ArgumentCaptor<Documento> documentoCaptor = ArgumentCaptor.forClass(Documento.class);
        verify(documentoRepository).save(documentoCaptor.capture());
        Documento capturedDocumento = documentoCaptor.getValue();
        assertThat(capturedDocumento.getNumeroVersiones()).isEqualTo(2);
        assertThat(capturedDocumento.getVersionActualId()).isEqualTo(201L);
    }
    
    @Test
    @DisplayName("should_ThrowResourceNotFoundException_When_DocumentNotFound_OnCreateVersion")
    void shouldThrowResourceNotFoundExceptionWhenDocumentNotFoundOnCreateVersion() {
        // Arrange
        org.springframework.web.multipart.MultipartFile mockFile = 
            mock(org.springframework.web.multipart.MultipartFile.class);
        com.docflow.documentcore.application.dto.CreateVersionRequest request = 
            new com.docflow.documentcore.application.dto.CreateVersionRequest(mockFile, "Test");
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.createVersion(DOCUMENTO_ID, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Documento no encontrado");
        
        // Verificar que no se intentó guardar nada
        verify(versionRepository, never()).save(any());
        verify(storageService, never()).upload(anyLong(), anyLong(), anyLong(), anyInt(), any(), anyLong());
    }
    
    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_UserLacksWritePermission_OnCreateVersion")
    void shouldThrowAccessDeniedExceptionWhenUserLacksWritePermissionOnCreateVersion() {
        // Arrange
        Documento documento = crearDocumento();
        org.springframework.web.multipart.MultipartFile mockFile = 
            mock(org.springframework.web.multipart.MultipartFile.class);
        com.docflow.documentcore.application.dto.CreateVersionRequest request = 
            new com.docflow.documentcore.application.dto.CreateVersionRequest(mockFile, "Test");
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.ESCRITURA, ORGANIZACION_ID))
            .thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> service.createVersion(DOCUMENTO_ID, request))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("No tiene permisos para crear versiones");
        
        // Verificar que no se intentó guardar nada
        verify(versionRepository, never()).save(any());
        verify(storageService, never()).upload(anyLong(), anyLong(), anyLong(), anyInt(), any(), anyLong());
    }
    
    @Test
    @DisplayName("should_ThrowStorageException_When_FileUploadFails_OnCreateVersion")
    void shouldThrowStorageExceptionWhenFileUploadFailsOnCreateVersion() throws Exception {
        // Arrange
        Documento documento = crearDocumento();
        org.springframework.web.multipart.MultipartFile mockFile = 
            mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getBytes()).thenReturn("content".getBytes());
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));
        
        com.docflow.documentcore.application.dto.CreateVersionRequest request = 
            new com.docflow.documentcore.application.dto.CreateVersionRequest(mockFile, "Test");
        
        when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tieneAcceso(
            USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, CodigoNivelAcceso.ESCRITURA, ORGANIZACION_ID))
            .thenReturn(true);
        when(storageService.upload(anyLong(), nullable(Long.class), anyLong(), anyInt(), any(), anyLong()))
            .thenThrow(new StorageException("Storage error"));
        
        // Act & Assert
        assertThatThrownBy(() -> service.createVersion(DOCUMENTO_ID, request))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("Storage error");
        
        // Verificar que no se guardó la versión
        verify(versionRepository, never()).save(any());
    }
    
    // ========================================================================
    // HELPERS
    // ========================================================================
    
    private Documento crearDocumento() {
        Documento documento = new Documento();
        documento.setId(DOCUMENTO_ID);
        documento.setOrganizacionId(ORGANIZACION_ID);
        documento.setNombre(NOMBRE_ARCHIVO);
        documento.setExtension(EXTENSION);
        documento.setTipoContenido(TIPO_CONTENIDO);
        documento.setTamanioBytes(TAMANIO_BYTES);
        documento.setVersionActualId(VERSION_ID);
        documento.setNumeroVersiones(1);
        documento.setFechaCreacion(OffsetDateTime.now());
        documento.setFechaActualizacion(OffsetDateTime.now());
        return documento;
    }
    
    private Version crearVersion() {
        Version version = new Version();
        version.setId(VERSION_ID);
        version.setDocumentoId(DOCUMENTO_ID);
        version.setNumeroSecuencial(1);
        version.setTamanioBytes(TAMANIO_BYTES);
        version.setRutaAlmacenamiento(RUTA_ALMACENAMIENTO);
        version.setHashContenido("abc123hash");
        version.setFechaCreacion(OffsetDateTime.now());
        return version;
    }
}
