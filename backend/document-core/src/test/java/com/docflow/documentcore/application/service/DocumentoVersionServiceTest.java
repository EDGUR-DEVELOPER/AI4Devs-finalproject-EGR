package com.docflow.documentcore.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.docflow.documentcore.application.dto.VersionItemResponse;
import com.docflow.documentcore.application.dto.VersionListResponse;
import com.docflow.documentcore.application.mapper.VersionListMapper;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;

/**
 * Tests unitarios para DocumentoVersionService - listado de versiones.
 * 
 * <p>Sigue enfoque TDD y valida:
 * <ul>
 *   <li>Caso exitoso sin paginación (retorna todas las versiones)</li>
 *   <li>Caso exitoso con paginación (metadatos correctos)</li>
 *   <li>Validación de existencia de documento</li>
 *   <li>Validación de tenant isolation (organizacionId)</li>
 *   <li>Validación de permisos (LECTURA mínimo requerido)</li>
 *   <li>Cálculo correcto del flag esVersionActual</li>
 *   <li>Cálculo correcto de metadatos de paginación</li>
 *   <li>Orden ascendente por número secuencial</li>
 * </ul>
 * 
 * <p><strong>US-DOC-004:</strong> Listar historial de versiones de documento.
 * 
 * @see DocumentoVersionService#listarVersiones(Long, Long, Long, Integer, Integer)
 */
@DisplayName("DocumentoVersionService - Listar Versiones")
@ExtendWith(MockitoExtension.class)
class DocumentoVersionServiceTest {
    
    @Mock
    private DocumentoRepository documentoRepository;
    
    @Mock
    private VersionRepository versionRepository;
    
    @Mock
    private IEvaluadorPermisos evaluadorPermisos;
    
    @Mock
    private VersionListMapper versionListMapper;
    
    private DocumentoVersionService service;
    
    private static final Long DOCUMENTO_ID = 100L;
    private static final Long VERSION_ACTUAL_ID = 203L;
    private static final Long USUARIO_ID = 1L;
    private static final Long ORGANIZACION_ID = 1L;
    
    @BeforeEach
    void setUp() {
        service = new DocumentoVersionService(
            documentoRepository,
            versionRepository,
            evaluadorPermisos,
            versionListMapper
        );
    }
    
    // ========================================================================
    // CASOS EXITOSOS - SIN PAGINACIÓN
    // ========================================================================
    
    @Nested
    @DisplayName("Sin Paginación")
    class SinPaginacion {
        
        @Test
        @DisplayName("should_ListAllVersions_When_NoPaginationProvided")
        void shouldListAllVersionsWhenNoPaginationProvided() {
            // Arrange
            Documento documento = crearDocumento(3);
            List<Version> versiones = crearVersiones(3);
            List<VersionItemResponse> versionesDto = crearVersionesDto(3);
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, NivelAcceso.LECTURA, ORGANIZACION_ID))
                .thenReturn(true);
            when(versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(DOCUMENTO_ID))
                .thenReturn(versiones);
            when(versionListMapper.toItemResponseList(versiones, VERSION_ACTUAL_ID))
                .thenReturn(versionesDto);
            
            // Act
            VersionListResponse result = service.listarVersiones(
                DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, null, null);
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getVersiones()).hasSize(3);
            assertThat(result.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
            assertThat(result.getTotalVersiones()).isEqualTo(3);
            assertThat(result.getPaginacion()).isNull();
            
            // Verificar interacciones
            verify(documentoRepository).findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID);
            verify(evaluadorPermisos).tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, NivelAcceso.LECTURA, ORGANIZACION_ID);
            verify(versionRepository).findByDocumentoIdOrderByNumeroSecuencialAsc(DOCUMENTO_ID);
            verify(versionRepository, never()).findByDocumentoIdOrderByNumeroSecuencialAsc(
                eq(DOCUMENTO_ID), any(Pageable.class));
        }
        
        @Test
        @DisplayName("should_ReturnEmptyList_When_DocumentHasNoVersions")
        void shouldReturnEmptyListWhenDocumentHasNoVersions() {
            // Arrange
            Documento documento = crearDocumento(0);
            List<Version> versionesVacias = List.of();
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, NivelAcceso.LECTURA, ORGANIZACION_ID))
                .thenReturn(true);
            when(versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(DOCUMENTO_ID))
                .thenReturn(versionesVacias);
            when(versionListMapper.toItemResponseList(versionesVacias, VERSION_ACTUAL_ID))
                .thenReturn(List.of());
            
            // Act
            VersionListResponse result = service.listarVersiones(
                DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, null, null);
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getVersiones()).isEmpty();
            assertThat(result.getTotalVersiones()).isZero();
        }
    }
    
    // ========================================================================
    // CASOS EXITOSOS - CON PAGINACIÓN
    // ========================================================================
    
    @Nested
    @DisplayName("Con Paginación")
    class ConPaginacion {
        
        @Test
        @DisplayName("should_ReturnFirstPage_When_PaginationProvided")
        void shouldReturnFirstPageWhenPaginationProvided() {
            // Arrange
            Documento documento = crearDocumento(5);
            List<Version> versionesPagina1 = crearVersiones(2);
            Page<Version> paginaVersiones = new PageImpl<>(
                versionesPagina1, 
                PageRequest.of(0, 2), 
                5  // Total 5 versiones
            );
            List<VersionItemResponse> versionesDto = crearVersionesDto(2);
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, NivelAcceso.LECTURA, ORGANIZACION_ID))
                .thenReturn(true);
            when(versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(
                eq(DOCUMENTO_ID), any(Pageable.class)))
                .thenReturn(paginaVersiones);
            when(versionListMapper.toItemResponseList(versionesPagina1, VERSION_ACTUAL_ID))
                .thenReturn(versionesDto);
            
            // Act
            VersionListResponse result = service.listarVersiones(
                DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, 1, 2);
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getVersiones()).hasSize(2);
            assertThat(result.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
            assertThat(result.getTotalVersiones()).isEqualTo(5);
            
            // Verificar metadatos de paginación
            assertThat(result.getPaginacion()).isNotNull();
            assertThat(result.getPaginacion().getPaginaActual()).isEqualTo(1);
            assertThat(result.getPaginacion().getTamanio()).isEqualTo(2);
            assertThat(result.getPaginacion().getTotalPaginas()).isEqualTo(3);
            assertThat(result.getPaginacion().getTotalElementos()).isEqualTo(5);
            assertThat(result.getPaginacion().getPrimeraPagina()).isTrue();
            assertThat(result.getPaginacion().getUltimaPagina()).isFalse();
        }
        
        @Test
        @DisplayName("should_ReturnLastPage_When_RequestingLastPage")
        void shouldReturnLastPageWhenRequestingLastPage() {
            // Arrange
            Documento documento = crearDocumento(5);
            List<Version> versionesPagina3 = crearVersiones(1);  // Última página con 1 elemento
            Page<Version> paginaVersiones = new PageImpl<>(
                versionesPagina3, 
                PageRequest.of(2, 2),  // Página 3 (índice 2) de tamaño 2
                5  // Total 5 versiones
            );
            List<VersionItemResponse> versionesDto = crearVersionesDto(1);
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, NivelAcceso.LECTURA, ORGANIZACION_ID))
                .thenReturn(true);
            when(versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(
                eq(DOCUMENTO_ID), any(Pageable.class)))
                .thenReturn(paginaVersiones);
            when(versionListMapper.toItemResponseList(versionesPagina3, VERSION_ACTUAL_ID))
                .thenReturn(versionesDto);
            
            // Act
            VersionListResponse result = service.listarVersiones(
                DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, 3, 2);
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getVersiones()).hasSize(1);
            assertThat(result.getTotalVersiones()).isEqualTo(5);
            
            // Verificar metadatos de última página
            assertThat(result.getPaginacion()).isNotNull();
            assertThat(result.getPaginacion().getPaginaActual()).isEqualTo(3);
            assertThat(result.getPaginacion().getTotalPaginas()).isEqualTo(3);
            assertThat(result.getPaginacion().getPrimeraPagina()).isFalse();
            assertThat(result.getPaginacion().getUltimaPagina()).isTrue();
        }
        
        @Test
        @DisplayName("should_ConvertPageNumber_From1BasedTo0Based")
        void shouldConvertPageNumberFrom1BasedTo0Based() {
            // Arrange
            Documento documento = crearDocumento(5);
            Page<Version> paginaVersiones = new PageImpl<>(
                crearVersiones(2),
                PageRequest.of(1, 2),  // Página 0-based = 1 → API 1-based = 2
                5
            );
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, NivelAcceso.LECTURA, ORGANIZACION_ID))
                .thenReturn(true);
            when(versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(
                eq(DOCUMENTO_ID), any(Pageable.class)))
                .thenReturn(paginaVersiones);
            when(versionListMapper.toItemResponseList(anyList(), eq(VERSION_ACTUAL_ID)))
                .thenReturn(crearVersionesDto(2));
            
            // Act
            service.listarVersiones(DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, 2, 2);
            
            // Assert
            verify(versionRepository).findByDocumentoIdOrderByNumeroSecuencialAsc(
                eq(DOCUMENTO_ID), 
                argThat(pageable -> pageable.getPageNumber() == 1)  // 0-based
            );
        }
    }
    
    // ========================================================================
    // CASOS DE ERROR - VALIDACIONES
    // ========================================================================
    
    @Nested
    @DisplayName("Validaciones de Error")
    class ValidacionesDeError {
        
        @Test
        @DisplayName("should_ThrowResourceNotFoundException_When_DocumentNotFound")
        void shouldThrowResourceNotFoundExceptionWhenDocumentNotFound() {
            // Arrange
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty());
            
            // Act & Assert
            assertThatThrownBy(() -> service.listarVersiones(
                DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Documento")
                .hasMessageContaining(String.valueOf(DOCUMENTO_ID));
            
            // No debe consultar versiones ni permisos
            verify(evaluadorPermisos, never()).tieneAcceso(
                anyLong(), anyLong(), any(), any(), anyLong());
            verify(versionRepository, never()).findByDocumentoIdOrderByNumeroSecuencialAsc(anyLong());
        }
        
        @Test
        @DisplayName("should_ThrowResourceNotFoundException_When_DocumentBelongsToDifferentOrganization")
        void shouldThrowResourceNotFoundExceptionWhenDocumentBelongsToDifferentOrganization() {
            // Arrange - Simular que el documento no se encuentra porque pertenece a otra organización
            Long otraOrganizacionId = 999L;
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, otraOrganizacionId))
                .thenReturn(Optional.empty());
            
            // Act & Assert
            assertThatThrownBy(() -> service.listarVersiones(
                DOCUMENTO_ID, USUARIO_ID, otraOrganizacionId, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Documento");
            
            // Verificar tenant isolation
            verify(documentoRepository).findByIdAndOrganizacionId(DOCUMENTO_ID, otraOrganizacionId);
            verify(evaluadorPermisos, never()).tieneAcceso(
                anyLong(), anyLong(), any(), any(), anyLong());
        }
        
        @Test
        @DisplayName("should_ThrowAccessDeniedException_When_UserHasNoReadPermission")
        void shouldThrowAccessDeniedExceptionWhenUserHasNoReadPermission() {
            // Arrange
            Documento documento = crearDocumento(3);
            
            when(documentoRepository.findByIdAndOrganizacionId(DOCUMENTO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(documento));
            when(evaluadorPermisos.tieneAcceso(
                USUARIO_ID, DOCUMENTO_ID, TipoRecurso.DOCUMENTO, NivelAcceso.LECTURA, ORGANIZACION_ID))
                .thenReturn(false);
            
            // Act & Assert
            assertThatThrownBy(() -> service.listarVersiones(
                DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permiso");
            
            // No debe consultar versiones
            verify(versionRepository, never()).findByDocumentoIdOrderByNumeroSecuencialAsc(anyLong());
            verify(versionRepository, never()).findByDocumentoIdOrderByNumeroSecuencialAsc(
                anyLong(), any(Pageable.class));
        }
    }
    
    // ========================================================================
    // MÉTODOS AUXILIARES
    // ========================================================================
    
    private Documento crearDocumento(int numeroVersiones) {
        Documento documento = new Documento();
        documento.setId(DOCUMENTO_ID);
        documento.setNombre("reporte-anual");
        documento.setExtension("pdf");
        documento.setVersionActualId(VERSION_ACTUAL_ID);
        documento.setOrganizacionId(ORGANIZACION_ID);
        documento.setNumeroVersiones(numeroVersiones);
        documento.setFechaCreacion(OffsetDateTime.now());
        return documento;
    }
    
    private List<Version> crearVersiones(int cantidad) {
        return IntStream.range(0, cantidad)
            .mapToObj(i -> {
                Version v = new Version();
                v.setId(200L + i);
                v.setDocumentoId(DOCUMENTO_ID);
                v.setNumeroSecuencial(i + 1);
                v.setTamanioBytes(1024000L);
                v.setHashContenido("hash" + i);
                v.setCreadoPor(USUARIO_ID);
                v.setFechaCreacion(OffsetDateTime.now());
                return v;
            })
            .toList();
    }
    
    private List<VersionItemResponse> crearVersionesDto(int cantidad) {
        return IntStream.range(0, cantidad)
            .mapToObj(i -> VersionItemResponse.builder()
                .id(200L + i)
                .numeroSecuencial(i + 1)
                .tamanioBytes(1024000L)
                .hashContenido("hash" + i)
                .comentarioCambio("Cambio " + (i + 1))
                .creadoPor(new VersionItemResponse.CreadorInfo(
                    USUARIO_ID, 
                    "Usuario Test", 
                    "test@docflow.com"))
                .fechaCreacion(OffsetDateTime.now())
                .descargas(0)
                .ultimaDescargaEn(null)
                .esVersionActual(200L + i == VERSION_ACTUAL_ID)
                .build())
            .toList();
    }
}
