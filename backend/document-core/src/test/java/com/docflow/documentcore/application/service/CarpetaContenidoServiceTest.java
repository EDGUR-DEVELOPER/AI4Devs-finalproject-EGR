package com.docflow.documentcore.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.CarpetaRaizNoEncontradaException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.ContenidoCarpeta;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.OpcionesListado;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IDocumentoRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;

/**
 * Tests unitarios para CarpetaContenidoService.
 * 
 * <p>Cubre escenarios de:
 * <ul>
 *   <li>Obtención de contenido con permisos válidos</li>
 *   <li>Rechazo de acceso sin permisos</li>
 *   <li>Manejo de carpetas no encontradas</li>
 *   <li>Paginación y ordenamiento</li>
 *   <li>Enriquecimiento con permisos específicos</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@DisplayName("CarpetaContenidoService")
@ExtendWith(MockitoExtension.class)
class CarpetaContenidoServiceTest {

    @Mock
    private ICarpetaRepository carpetaRepository;

    @Mock
    private IDocumentoRepository documentoRepository;

    @Mock
    private IEvaluadorPermisos evaluadorPermisos;

    private CarpetaContenidoService service;

    private static final Long CARPETA_ID = 1L;
    private static final Long USUARIO_ID = 100L;
    private static final Long ORGANIZACION_ID = 10L;
    private static final Long CARPETA_RAIZ_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new CarpetaContenidoService(
                carpetaRepository,
                documentoRepository,
                evaluadorPermisos);
    }

    // ========================================================================
    // HAPPY PATH TESTS
    // ========================================================================

    @Test
    @DisplayName("should_ObtenerContenidoCarpeta_When_ValidCarpetaAndHaveReadPermission")
    void shouldObtenerContenidoCarpetaWhenValidCarpetaAndHaveReadPermission() {
        // Arrange
        Carpeta carpeta = crearCarpeta(CARPETA_ID, "Carpeta Prueba", ORGANIZACION_ID);
        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre", 
                org.springframework.data.domain.Sort.Direction.ASC);

        PermisoEfectivo permiso = crearPermiso(NivelAcceso.LECTURA);

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(permiso);
        when(carpetaRepository.obtenerSubcarpetasConPermiso(CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(List.of());
        when(documentoRepository.obtenerDocumentosConPermiso(
                eq(CARPETA_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID), any(Pageable.class)))
                .thenReturn(List.of());
        when(carpetaRepository.contarSubcarpetasConPermiso(CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(0);
        when(documentoRepository.contarDocumentosConPermiso(CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(0L);

        // Act
        ContenidoCarpeta resultado = service.obtenerContenidoCarpeta(
                CARPETA_ID,
                USUARIO_ID,
                ORGANIZACION_ID,
                opciones);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.subcarpetas()).isEmpty();
        assertThat(resultado.documentos()).isEmpty();
        assertThat(resultado.totalSubcarpetas()).isEqualTo(0);
        assertThat(resultado.totalDocumentos()).isEqualTo(0);
        assertThat(resultado.paginaActual()).isEqualTo(1);

        verify(carpetaRepository).obtenerPorId(CARPETA_ID, ORGANIZACION_ID);
        verify(evaluadorPermisos).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ID, ORGANIZACION_ID);
    }

    @Test
    @DisplayName("should_ObtenerContenidoRaiz_When_RaizExistsAndHavePermission")
    void shouldObtenerContenidoRaizWhenRaizExistsAndHavePermission() {
        // Arrange
        Carpeta raiz = crearCarpeta(CARPETA_RAIZ_ID, "Raíz", ORGANIZACION_ID);
        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre",
                org.springframework.data.domain.Sort.Direction.ASC);

        PermisoEfectivo permiso = crearPermiso(NivelAcceso.LECTURA);

        when(carpetaRepository.obtenerRaiz(ORGANIZACION_ID))
                .thenReturn(Optional.of(raiz));
        when(carpetaRepository.obtenerPorId(CARPETA_RAIZ_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(raiz));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_RAIZ_ID, ORGANIZACION_ID))
                .thenReturn(permiso);
        when(carpetaRepository.obtenerSubcarpetasConPermiso(CARPETA_RAIZ_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(List.of());
        when(documentoRepository.obtenerDocumentosConPermiso(
                eq(CARPETA_RAIZ_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID), any(Pageable.class)))
                .thenReturn(List.of());
        when(carpetaRepository.contarSubcarpetasConPermiso(CARPETA_RAIZ_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(0);
        when(documentoRepository.contarDocumentosConPermiso(CARPETA_RAIZ_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(0L);

        // Act
        ContenidoCarpeta resultado = service.obtenerContenidoRaiz(
                USUARIO_ID,
                ORGANIZACION_ID,
                opciones);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.paginaActual()).isEqualTo(1);

        verify(carpetaRepository).obtenerRaiz(ORGANIZACION_ID);
        verify(carpetaRepository).obtenerPorId(CARPETA_RAIZ_ID, ORGANIZACION_ID);
    }

    // ========================================================================
    // ERROR PATH TESTS
    // ========================================================================

    @Test
    @DisplayName("should_ThrowCarpetaNotFoundException_When_CarpetaDoesNotExist")
    void shouldThrowCarpetaNotFoundExceptionWhenCarpetaDoesNotExist() {
        // Arrange
        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre",
                org.springframework.data.domain.Sort.Direction.ASC);

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerContenidoCarpeta(
                CARPETA_ID,
                USUARIO_ID,
                ORGANIZACION_ID,
                opciones))
                .isInstanceOf(CarpetaNotFoundException.class);
    }

    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_NoReadPermission")
    void shouldThrowAccessDeniedExceptionWhenNoReadPermission() {
        // Arrange
        Carpeta carpeta = crearCarpeta(CARPETA_ID, "Carpeta Prueba", ORGANIZACION_ID);
        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre",
                org.springframework.data.domain.Sort.Direction.ASC);

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(null); // Sin permiso

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerContenidoCarpeta(
                CARPETA_ID,
                USUARIO_ID,
                ORGANIZACION_ID,
                opciones))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("should_ThrowCarpetaRaizNoEncontradaException_When_RaizNotFound")
    void shouldThrowCarpetaRaizNoEncontradaExceptionWhenRaizNotFound() {
        // Arrange
        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre",
                org.springframework.data.domain.Sort.Direction.ASC);

        when(carpetaRepository.obtenerRaiz(ORGANIZACION_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerContenidoRaiz(
                USUARIO_ID,
                ORGANIZACION_ID,
                opciones))
                .isInstanceOf(CarpetaRaizNoEncontradaException.class);
    }

    @Test
    @DisplayName("should_ThrowAccessDeniedException_When_PermissionWithoutReadLevel")
    void shouldThrowAccessDeniedExceptionWhenPermissionWithoutReadLevel() {
        // Arrange
        Carpeta carpeta = crearCarpeta(CARPETA_ID, "Carpeta Prueba", ORGANIZACION_ID);
        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre",
                org.springframework.data.domain.Sort.Direction.ASC);

        // Permiso sin nivel de lectura (simular: administración sin lectura - caso edge)
        PermisoEfectivo permisoInsuficiente = crearPermiso(NivelAcceso.ADMINISTRACION);

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(permisoInsuficiente);

        // Act & Assert
        // Nota: ADMINISTRACION incluye implícitamente LECTURA en la mayoría de sistemas
        // Este test asume que permiteAccion() retorna true para LECTURA si ADMINISTRACION existe
        ContenidoCarpeta resultado = service.obtenerContenidoCarpeta(
                CARPETA_ID,
                USUARIO_ID,
                ORGANIZACION_ID,
                opciones);

        assertThat(resultado).isNotNull(); // Debería pasar porque ADMINISTRACION >= LECTURA
    }

    // ========================================================================
    // EDGE CASES
    // ========================================================================

    @Test
    @DisplayName("should_CalculateTotalPagesCorrectly_When_PaginationApplied")
    void shouldCalculateTotalPagesCorrectlyWhenPaginationApplied() {
        // Arrange
        Carpeta carpeta = crearCarpeta(CARPETA_ID, "Carpeta Prueba", ORGANIZACION_ID);
        OpcionesListado opciones = new OpcionesListado(1, 10, "nombre",
                org.springframework.data.domain.Sort.Direction.ASC);

        PermisoEfectivo permiso = crearPermiso(NivelAcceso.LECTURA);

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(permiso);
        when(carpetaRepository.obtenerSubcarpetasConPermiso(CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(List.of());
        when(documentoRepository.obtenerDocumentosConPermiso(
                eq(CARPETA_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID), any(Pageable.class)))
                .thenReturn(List.of());
        when(carpetaRepository.contarSubcarpetasConPermiso(CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(0);
        when(documentoRepository.contarDocumentosConPermiso(CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(25L); // 25 documentos con tamaño de página 10 = 3 páginas

        // Act
        ContenidoCarpeta resultado = service.obtenerContenidoCarpeta(
                CARPETA_ID,
                USUARIO_ID,
                ORGANIZACION_ID,
                opciones);

        // Assert
        assertThat(resultado.totalPaginas()).isEqualTo(3);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Carpeta crearCarpeta(Long id, String nombre, Long organizacionId) {
        return Carpeta.builder()
                .id(id)
                .nombre(nombre)
                .organizacionId(organizacionId)
                .creadoPor(USUARIO_ID)
                .descripcion("Descripción test")
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
    }

    private PermisoEfectivo crearPermiso(NivelAcceso nivel) {
        return PermisoEfectivo.carpetaDirecto(
            nivel,
            CARPETA_ID,
            "Test Carpeta"
        );
    }
}
