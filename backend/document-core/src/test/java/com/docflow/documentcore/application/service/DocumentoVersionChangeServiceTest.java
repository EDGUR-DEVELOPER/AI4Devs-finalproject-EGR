package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.DocumentoResponse;
import com.docflow.documentcore.domain.exception.InsufficientPermissionsException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.VersionNotBelongToDocumentException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import com.docflow.documentcore.domain.service.VersionChangeEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para DocumentoVersionChangeService.
 * 
 * <p>Prueba la lógica de cambio de versión actual (rollback) con validaciones
 * de permisos, aislamiento multi-tenant y publicación de eventos de auditoría.</p>
 */
@DisplayName("DocumentoVersionChangeService - Cambiar Versión Actual")
@ExtendWith(MockitoExtension.class)
class DocumentoVersionChangeServiceTest {
    
    @Mock
    private DocumentoRepository documentoRepository;
    
    @Mock
    private VersionRepository versionRepository;
    
    @Mock
    private IEvaluadorPermisos evaluadorPermisos;
    
    @Mock
    private VersionChangeEventPublisher auditPublisher;
    
    @InjectMocks
    private DocumentoVersionChangeService service;
    
    private static final Long DOCUMENTO_ID = 100L;
    private static final Long VERSION_ID = 201L;
    private static final Long VERSION_ANTERIOR_ID = 205L;
    private static final Long USUARIO_ID = 1L;
    private static final Long ORGANIZACION_ID = 1L;
    
    private Documento documento;
    private Version version;
    
    @BeforeEach
    void setUp() {
        // Documento mock con versión actual 205
        documento = new Documento();
        documento.setId(DOCUMENTO_ID);
        documento.setOrganizacionId(ORGANIZACION_ID);
        documento.setNombre("documento-test.pdf");
        documento.setExtension("pdf");
        documento.setTipoContenido("application/pdf");
        documento.setTamanioBytes(1024L);
        documento.setCarpetaId(10L);
        documento.setVersionActualId(VERSION_ANTERIOR_ID);
        documento.setNumeroVersiones(5);
        documento.setBloqueado(false);
        documento.setFechaCreacion(OffsetDateTime.now().minusDays(10));
        documento.setFechaActualizacion(OffsetDateTime.now().minusDays(2));
        
        // Versión mock (versión 201 a la que queremos hacer rollback)
        version = new Version();
        version.setId(VERSION_ID);
        version.setDocumentoId(DOCUMENTO_ID);
        version.setNumeroSecuencial(2);
        version.setTamanioBytes(1024L);
        version.setHashContenido("abc123hash");
        version.setFechaCreacion(OffsetDateTime.now().minusDays(8));
    }
    
    @Nested
    @DisplayName("Casos Exitosos")
    class CasosExitosos {
        
        @Test
        @DisplayName("should_ChangeCurrentVersion_When_AllValidationsPass")
        void shouldChangeCurrentVersionWhenAllValidationsPass() {
            // Given
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, 
                CodigoNivelAcceso.ADMINISTRACION, ORGANIZACION_ID))
                .thenReturn(true);
            when(versionRepository.findByIdAndDocumentoId(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
            when(documentoRepository.save(any(Documento.class)))
                .thenReturn(documento);
            
            // When
            DocumentoResponse response = service.cambiarVersionActual(
                DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID
            );
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(DOCUMENTO_ID);
            assertThat(response.getVersionActual()).isNotNull();
            assertThat(response.getVersionActual().getId()).isEqualTo(VERSION_ID);
            assertThat(response.getNumeroVersiones()).isEqualTo(5);
            
            // Verify documento fue actualizado
            verify(documentoRepository).save(any(Documento.class));
            ArgumentCaptor<Documento> docCaptor = ArgumentCaptor.forClass(Documento.class);
            verify(documentoRepository).save(docCaptor.capture());
            assertThat(docCaptor.getValue().getVersionActualId()).isEqualTo(VERSION_ID);
            assertThat(docCaptor.getValue().getFechaActualizacion()).isNotNull();
            
            // Verify evento de auditoría fue publicado
            verify(auditPublisher).publishVersionRollbackEvent(
                eq(USUARIO_ID), eq(DOCUMENTO_ID), eq(ORGANIZACION_ID),
                eq(VERSION_ANTERIOR_ID), eq(VERSION_ID), any(OffsetDateTime.class)
            );
        }
        
        @Test
        @DisplayName("should_PublishAuditEvent_When_VersionChanged")
        void shouldPublishAuditEventWhenVersionChanged() {
            // Given
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(anyLong(), anyLong(), any(), any(), anyLong()))
                .thenReturn(true);
            when(versionRepository.findByIdAndDocumentoId(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
            when(documentoRepository.save(any(Documento.class)))
                .thenReturn(documento);
            
            // When
            service.cambiarVersionActual(DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID);
            
            // Then - Verificar que evento contiene todos los campos correctos
            ArgumentCaptor<Long> usuarioCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> documentoCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> organizacionCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> versionAnteriorCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> versionNuevaCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<OffsetDateTime> timestampCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            
            verify(auditPublisher).publishVersionRollbackEvent(
                usuarioCaptor.capture(),
                documentoCaptor.capture(),
                organizacionCaptor.capture(),
                versionAnteriorCaptor.capture(),
                versionNuevaCaptor.capture(),
                timestampCaptor.capture()
            );
            
            assertThat(usuarioCaptor.getValue()).isEqualTo(USUARIO_ID);
            assertThat(documentoCaptor.getValue()).isEqualTo(DOCUMENTO_ID);
            assertThat(organizacionCaptor.getValue()).isEqualTo(ORGANIZACION_ID);
            assertThat(versionAnteriorCaptor.getValue()).isEqualTo(VERSION_ANTERIOR_ID);
            assertThat(versionNuevaCaptor.getValue()).isEqualTo(VERSION_ID);
            assertThat(timestampCaptor.getValue()).isNotNull();
        }
        
        @Test
        @DisplayName("should_BeIdempotent_When_ChangingToSameCurrentVersion")
        void shouldBeIdempotentWhenChangingToSameCurrentVersion() {
            // Given - versión solicitada ya es la versión actual
            documento.setVersionActualId(VERSION_ID);
            version.setId(VERSION_ID);
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(anyLong(), anyLong(), any(), any(), anyLong()))
                .thenReturn(true);
            when(versionRepository.findByIdAndDocumentoId(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.of(version));
            when(documentoRepository.save(any(Documento.class)))
                .thenReturn(documento);
            
            // When
            DocumentoResponse response = service.cambiarVersionActual(
                DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID
            );
            
            // Then - operación completada sin error
            assertThat(response).isNotNull();
            assertThat(response.getVersionActual().getId()).isEqualTo(VERSION_ID);
            
            // Verify documento fue guardado (actualización de fecha)
            verify(documentoRepository).save(any(Documento.class));
            
            // Verify evento de auditoría fue publicado igual (para trazabilidad)
            verify(auditPublisher).publishVersionRollbackEvent(
                eq(USUARIO_ID), eq(DOCUMENTO_ID), eq(ORGANIZACION_ID),
                eq(VERSION_ID), eq(VERSION_ID), any(OffsetDateTime.class)
            );
        }
    }
    
    @Nested
    @DisplayName("Validaciones de Error")
    class ValidacionesDeError {
        
        @Test
        @DisplayName("should_ThrowResourceNotFoundException_When_DocumentNotFound")
        void shouldThrowResourceNotFoundExceptionWhenDocumentNotFound() {
            // Given
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> service.cambiarVersionActual(
                DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID
            ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Documento")
                .hasMessageContaining(DOCUMENTO_ID.toString());
            
            // Verify no se intentó guardar ni publicar evento
            verify(documentoRepository, never()).save(any());
            verify(auditPublisher, never()).publishVersionRollbackEvent(
                anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), any()
            );
        }
        
        @Test
        @DisplayName("should_ThrowInsufficientPermissionsException_When_UserLacksAdminPermission")
        void shouldThrowInsufficientPermissionsExceptionWhenUserLacksAdminPermission() {
            // Given
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO,
                CodigoNivelAcceso.ADMINISTRACION, ORGANIZACION_ID))
                .thenReturn(false);
            
            // When & Then
            assertThatThrownBy(() -> service.cambiarVersionActual(
                DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID
            ))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessageContaining("ADMINISTRACION")
                .hasMessageContaining("DOCUMENTO");
            
            // Verify no se intentó guardar ni publicar evento
            verify(documentoRepository, never()).save(any());
            verify(auditPublisher, never()).publishVersionRollbackEvent(
                anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), any()
            );
        }
        
        @Test
        @DisplayName("should_ThrowVersionNotBelongToDocumentException_When_VersionNotFound")
        void shouldThrowVersionNotBelongToDocumentExceptionWhenVersionNotFound() {
            // Given
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(anyLong(), anyLong(), any(), any(), anyLong()))
                .thenReturn(true);
            when(versionRepository.findByIdAndDocumentoId(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> service.cambiarVersionActual(
                DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID
            ))
                .isInstanceOf(VersionNotBelongToDocumentException.class)
                .hasMessageContaining(VERSION_ID.toString())
                .hasMessageContaining(DOCUMENTO_ID.toString());
            
            // Verify no se guardó ni publicó evento
            verify(documentoRepository, never()).save(any());
            verify(auditPublisher, never()).publishVersionRollbackEvent(
                anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), any()
            );
        }
        
        @Test
        @DisplayName("should_ThrowVersionNotBelongToDocumentException_When_VersionBelongsToDifferentDocument")
        void shouldThrowVersionNotBelongToDocumentExceptionWhenVersionBelongsToDifferentDocument() {
            // Given
            Version versionDeOtroDocumento = new Version();
            versionDeOtroDocumento.setId(VERSION_ID);
            versionDeOtroDocumento.setDocumentoId(999L); // Otro documento
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(anyLong(), anyLong(), any(), any(), anyLong()))
                .thenReturn(true);
            when(versionRepository.findByIdAndDocumentoId(VERSION_ID, DOCUMENTO_ID))
                .thenReturn(Optional.empty()); // No encuentra porque pertenece a otro doc
            
            // When & Then
            assertThatThrownBy(() -> service.cambiarVersionActual(
                DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID
            ))
                .isInstanceOf(VersionNotBelongToDocumentException.class);
            
            // Verify no se guardó ni publicó evento
            verify(documentoRepository, never()).save(any());
            verify(auditPublisher, never()).publishVersionRollbackEvent(
                anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), any()
            );
        }
    }
    
    @Nested
    @DisplayName("Validaciones Multi-Tenant")
    class ValidacionesMultiTenant {
        
        @Test
        @DisplayName("should_ThrowResourceNotFoundException_When_DocumentBelongsToDifferentOrganization")
        void shouldThrowResourceNotFoundExceptionWhenDocumentBelongsToDifferentOrganization() {
            // Given - documento existe pero es de otra organización
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty()); // No encuentra por organizacionId diferente
            
            // When & Then
            assertThatThrownBy(() -> service.cambiarVersionActual(
                DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID
            ))
                .isInstanceOf(ResourceNotFoundException.class);
            
            // Verify no revelar existencia del documento en otra org
            verify(evaluadorPermisos, never()).tieneAcceso(anyLong(), anyLong(), any(), any(), anyLong());
            verify(documentoRepository, never()).save(any());
            verify(auditPublisher, never()).publishVersionRollbackEvent(
                anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), any()
            );
        }
    }
}
