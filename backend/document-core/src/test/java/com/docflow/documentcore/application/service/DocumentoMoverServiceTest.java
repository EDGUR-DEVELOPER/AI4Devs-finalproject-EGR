package com.docflow.documentcore.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.docflow.documentcore.application.dto.DocumentoMovidoResponse;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.MismaUbicacionException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IDocumentoRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;

/**
 * Tests unitarios para DocumentoMoverService.
 * 
 * <p>Sigue enfoque TDD y valida:
 * <ul>
 *   <li>Caso exitoso con permisos ESCRITURA</li>
 *   <li>Caso exitoso con permisos ADMINISTRACION</li>
 *   <li>Validaciones de existencia (documento, carpeta destino)</li>
 *   <li>Regla de negocio: no mover a misma carpeta</li>
 *   <li>Validaciones de permisos duales (origen y destino)</li>
 *   <li>Rechazo por permisos insuficientes (LECTURA, null)</li>
 * </ul>
 * 
 * @see DocumentoMoverService
 */
@DisplayName("DocumentoMoverService")
@ExtendWith(MockitoExtension.class)
class DocumentoMoverServiceTest {
    
    @Mock
    private IDocumentoRepository documentoRepository;
    
    @Mock
    private ICarpetaRepository carpetaRepository;
    
    @Mock
    private IEvaluadorPermisos evaluadorPermisos;
    
    private DocumentoMoverService service;
    
    private static final Long DOCUMENTO_ID = 100L;
    private static final Long CARPETA_ORIGEN_ID = 10L;
    private static final Long CARPETA_DESTINO_ID = 25L;
    private static final Long USUARIO_ID = 1L;
    private static final Long ORGANIZACION_ID = 1L;
    
    @BeforeEach
    void setUp() {
        service = new DocumentoMoverService(
            documentoRepository,
            carpetaRepository,
            evaluadorPermisos
        );
    }
    
    // ========================================================================
    // CASOS EXITOSOS
    // ========================================================================
    
    @Test
    @DisplayName("should_MoverDocumento_When_AllValidationsPass")
    void shouldMoverDocumentoWhenAllValidationsPass() {
        // Arrange
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
        PermisoEfectivo permisoEscritura = crearPermiso(NivelAcceso.ESCRITURA);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpetaDestino));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                .thenReturn(permisoEscritura);
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(permisoEscritura);
        
        // Act
        DocumentoMovidoResponse response = service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        );
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
        assertThat(response.getCarpetaOrigenId()).isEqualTo(CARPETA_ORIGEN_ID);
        assertThat(response.getCarpetaDestinoId()).isEqualTo(CARPETA_DESTINO_ID);
        assertThat(response.getMensaje()).contains("exitosamente");
        
        // Verificar que el documento fue actualizado
        assertThat(documento.getCarpetaId()).isEqualTo(CARPETA_DESTINO_ID);
        
        verify(documentoRepository).obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID);
        verify(carpetaRepository).obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID);
        verify(evaluadorPermisos, times(2)).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
    }
    
    @Test
    @DisplayName("should_AllowMove_When_UserHasAdministrationPermission")
    void shouldAllowMoveWhenUserHasAdministrationPermission() {
        // Arrange - El nivel ADMINISTRACION debe permitir operaciones de ESCRITURA
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
        PermisoEfectivo permisoAdmin = crearPermiso(NivelAcceso.ADMINISTRACION);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpetaDestino));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                .thenReturn(permisoAdmin);
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(permisoAdmin);
        
        // Act
        DocumentoMovidoResponse response = service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        );
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(documento.getCarpetaId()).isEqualTo(CARPETA_DESTINO_ID);
    }
    
    // ========================================================================
    // ERRORES DE VALIDACIÓN - EXISTENCIA
    // ========================================================================
    
    @Test
    @DisplayName("should_ThrowResourceNotFoundException_When_DocumentoDoesNotExist")
    void shouldThrowResourceNotFoundExceptionWhenDocumentoDoesNotExist() {
        // Arrange
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        ))
        .isInstanceOf(ResourceNotFoundException.class);
        
        verify(carpetaRepository, never()).obtenerPorId(anyLong(), anyLong());
        verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
    }
    
    @Test
    @DisplayName("should_ThrowCarpetaNotFoundException_When_CarpetaDestinoDoesNotExist")
    void shouldThrowCarpetaNotFoundExceptionWhenCarpetaDestinoDoesNotExist() {
        // Arrange
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        ))
        .isInstanceOf(CarpetaNotFoundException.class);
        
        verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
    }
    
    // ========================================================================
    // ERRORES DE REGLA DE NEGOCIO
    // ========================================================================
    
    @Test
    @DisplayName("should_ThrowMismaUbicacionException_When_MovingToSameFolder")
    void shouldThrowMismaUbicacionExceptionWhenMovingToSameFolder() {
        // Arrange
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        Carpeta carpeta = crearCarpeta(CARPETA_ORIGEN_ID);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        
        // Act & Assert
        assertThatThrownBy(() -> service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_ORIGEN_ID, // Same as origin
            USUARIO_ID,
            ORGANIZACION_ID
        ))
        .isInstanceOf(MismaUbicacionException.class);
        
        verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
    }
    
    // ========================================================================
    // ERRORES DE PERMISOS
    // ========================================================================
    
    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_NoWritePermissionOnOrigin")
    void shouldThrowAccessDeniedExceptionWhenNoWritePermissionOnOrigin() {
        // Arrange
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
        PermisoEfectivo permisoLecturaOnly = crearPermiso(NivelAcceso.LECTURA);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpetaDestino));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                .thenReturn(permisoLecturaOnly); // Only READ, not WRITE
        
        // Act & Assert
        assertThatThrownBy(() -> service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        ))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("origen");
        
        verify(evaluadorPermisos).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID);
        verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID);
    }
    
    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_NoWritePermissionOnDestination")
    void shouldThrowAccessDeniedExceptionWhenNoWritePermissionOnDestination() {
        // Arrange
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
        PermisoEfectivo permisoEscritura = crearPermiso(NivelAcceso.ESCRITURA);
        PermisoEfectivo permisoLecturaOnly = crearPermiso(NivelAcceso.LECTURA);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpetaDestino));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                .thenReturn(permisoEscritura); // WRITE on origin
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(permisoLecturaOnly); // Only READ on destination
        
        // Act & Assert
        assertThatThrownBy(() -> service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        ))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("destino");
        
        verify(evaluadorPermisos).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID);
        verify(evaluadorPermisos).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID);
    }
    
    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_NoPermissionOnOrigin")
    void shouldThrowAccessDeniedExceptionWhenNoPermissionOnOrigin() {
        // Arrange
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpetaDestino));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                .thenReturn(null); // No permission at all
        
        // Act & Assert
        assertThatThrownBy(() -> service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        ))
        .isInstanceOf(AccessDeniedException.class);
    }
    
    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_NoPermissionOnDestination")
    void shouldThrowAccessDeniedExceptionWhenNoPermissionOnDestination() {
        // Arrange
        Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
        Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
        PermisoEfectivo permisoEscritura = crearPermiso(NivelAcceso.ESCRITURA);
        
        when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
        when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpetaDestino));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                .thenReturn(permisoEscritura);
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID))
                .thenReturn(null); // No permission at all
        
        // Act & Assert
        assertThatThrownBy(() -> service.moverDocumento(
            DOCUMENTO_ID,
            CARPETA_DESTINO_ID,
            USUARIO_ID,
            ORGANIZACION_ID
        ))
        .isInstanceOf(AccessDeniedException.class);
    }
    
    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================
    
    private Documento crearDocumento(Long id, Long carpetaId) {
        Documento doc = new Documento();
        doc.setId(id);
        doc.setCarpetaId(carpetaId);
        doc.setOrganizacionId(ORGANIZACION_ID);
        doc.setNombre("test-documento.pdf");
        doc.setCreadoPor(USUARIO_ID);
        doc.setFechaCreacion(OffsetDateTime.now());
        doc.setFechaActualizacion(OffsetDateTime.now());
        return doc;
    }
    
    private Carpeta crearCarpeta(Long id) {
        return Carpeta.builder()
                .id(id)
                .nombre("Test Carpeta")
                .organizacionId(ORGANIZACION_ID)
                .creadoPor(USUARIO_ID)
                .build();
    }
    
    private PermisoEfectivo crearPermiso(NivelAcceso nivel) {
        return PermisoEfectivo.carpetaDirecto(
            nivel,
            CARPETA_ORIGEN_ID,
            "Test Carpeta"
        );
    }
}
