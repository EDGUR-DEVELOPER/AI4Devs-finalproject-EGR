package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.validator.CarpetaValidator;
import com.docflow.documentcore.application.validator.PermisoCarpetaUsuarioValidator;
import com.docflow.documentcore.domain.event.CarpetaCreatedEvent;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNombreDuplicadoException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CarpetaService.
 * 
 * <p>Sigue enfoque TDD y valida:
 * <ul>
 *   <li>Casos exitosos (happy path)</li>
 *   <li>Validaciones de negocio</li>
 *   <li>Manejo de excepciones</li>
 *   <li>Emisión de eventos</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarpetaService - Tests Unitarios")
class CarpetaServiceTest {
    
    @Mock
    private ICarpetaRepository carpetaRepository;
    
    @Mock
    private CarpetaValidator validator;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

        @Mock
        private IEvaluadorPermisos evaluadorPermisos;

        @Mock
        private IPermisoCarpetaUsuarioRepository permisoRepository;

        @Mock
        private PermisoCarpetaUsuarioValidator permisoValidator;

        @Mock
        private UsuarioJpaRepository usuarioRepository;

        @Mock
        private NivelAccesoService nivelAccesoService;

    private CarpetaService carpetaService;
    
    private Long organizacionId;
    private Long usuarioId;
    private Long carpetaPadreId;
    
    @BeforeEach
    void setUp() {
        organizacionId = new Random().nextLong();
        usuarioId = new Random().nextLong();
        carpetaPadreId = new Random().nextLong();

        PermisoCarpetaUsuarioService permisoCarpetaUsuarioService = new PermisoCarpetaUsuarioService(
                permisoRepository,
                permisoValidator,
                usuarioRepository,
                nivelAccesoService,
                eventPublisher
        );

        carpetaService = new CarpetaService(
                carpetaRepository,
                validator,
                eventPublisher,
                evaluadorPermisos,
                permisoCarpetaUsuarioService
        );
    }
    
    // ========================================================================
    // TESTS DE CREACIÓN EXITOSA
    // ========================================================================
    
    @Test
    @DisplayName("Debería crear carpeta exitosamente con datos válidos")
    void should_CreateCarpeta_When_DataIsValid() {
        // Arrange
        String nombre = "Proyecto Alpha";
        String descripcion = "Documentos del proyecto Alpha";
        
        Carpeta carpetaEsperada = Carpeta.builder()
                .id(new Random().nextLong())
                .nombre(nombre)
                .descripcion(descripcion)
                .carpetaPadreId(carpetaPadreId)
                .organizacionId(organizacionId)
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
        
        when(carpetaRepository.crear(any(Carpeta.class))).thenReturn(carpetaEsperada);
        
        // Act
        Carpeta resultado = carpetaService.crear(
                nombre, 
                descripcion, 
                carpetaPadreId, 
                organizacionId, 
                usuarioId
        );
        
        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNombre()).isEqualTo(nombre);
        assertThat(resultado.getDescripcion()).isEqualTo(descripcion);
        assertThat(resultado.getCarpetaPadreId()).isEqualTo(carpetaPadreId);
        assertThat(resultado.getOrganizacionId()).isEqualTo(organizacionId);
        assertThat(resultado.getCreadoPor()).isEqualTo(usuarioId);
        
        // Verify validaciones fueron llamadas
        verify(validator).validarCarpetaPadreExiste(carpetaPadreId, organizacionId);
        verify(validator).validarPermisos(usuarioId, carpetaPadreId, organizacionId);
        verify(validator).validarNombreUnico(nombre, carpetaPadreId, organizacionId);
        
        // Verify persistencia
        verify(carpetaRepository).crear(any(Carpeta.class));
        
        // Verify evento emitido
        verify(eventPublisher).publishEvent(any(CarpetaCreatedEvent.class));
    }
    
    @Test
    @DisplayName("Debería crear carpeta sin descripción (campo opcional)")
    void should_CreateCarpeta_When_DescriptionIsNull() {
        // Arrange
        String nombre = "Sin Descripción";
        
        Carpeta carpetaEsperada = Carpeta.builder()
                .id(new Random().nextLong())
                .nombre(nombre)
                .descripcion(null)
                .carpetaPadreId(carpetaPadreId)
                .organizacionId(organizacionId)
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
        
        when(carpetaRepository.crear(any(Carpeta.class))).thenReturn(carpetaEsperada);
        
        // Act
        Carpeta resultado = carpetaService.crear(
                nombre, 
                null,  // Sin descripción
                carpetaPadreId, 
                organizacionId, 
                usuarioId
        );
        
        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getDescripcion()).isNull();
    }
    
    // ========================================================================
    // TESTS DE VALIDACIONES
    // ========================================================================
    
    @Test
    @DisplayName("Debería fallar cuando la carpeta padre no existe")
    void should_ThrowException_When_ParentFolderNotFound() {
        // Arrange
        String nombre = "Test Folder";
        
        doThrow(new CarpetaNotFoundException(carpetaPadreId))
                .when(validator).validarCarpetaPadreExiste(carpetaPadreId, organizacionId);
        
        // Act & Assert
        assertThatThrownBy(() -> 
            carpetaService.crear(nombre, null, carpetaPadreId, organizacionId, usuarioId)
        )
        .isInstanceOf(CarpetaNotFoundException.class)
        .hasMessageContaining(carpetaPadreId.toString());
        
        // Verify no se intentó persistir
        verify(carpetaRepository, never()).crear(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
    
    @Test
    @DisplayName("Debería fallar cuando el nombre ya existe en el nivel")
    void should_ThrowException_When_NameAlreadyExists() {
        // Arrange
        String nombreDuplicado = "Carpeta Existente";
        
        doThrow(new CarpetaNombreDuplicadoException(nombreDuplicado, carpetaPadreId))
                .when(validator).validarNombreUnico(nombreDuplicado, carpetaPadreId, organizacionId);
        
        // Act & Assert
        assertThatThrownBy(() -> 
            carpetaService.crear(nombreDuplicado, null, carpetaPadreId, organizacionId, usuarioId)
        )
        .isInstanceOf(CarpetaNombreDuplicadoException.class)
        .hasMessageContaining(nombreDuplicado);
        
        // Verify no se intentó persistir
        verify(carpetaRepository, never()).crear(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
    
    // ========================================================================
    // TESTS DE CONSULTAS
    // ========================================================================
    
    @Test
    @DisplayName("Debería obtener carpeta por ID correctamente")
    void should_GetCarpetaById_When_Exists() {
        // Arrange
        Long carpetaId = new Random().nextLong();
        Carpeta carpetaEsperada = Carpeta.builder()
                .id(carpetaId)
                .nombre("Test")
                .organizacionId(organizacionId)
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
        
        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpetaEsperada));
        
        // Act
        Carpeta resultado = carpetaService.obtenerPorId(carpetaId, organizacionId);
        
        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(carpetaId);
        verify(carpetaRepository).obtenerPorId(carpetaId, organizacionId);
    }
    
    @Test
    @DisplayName("Debería lanzar excepción cuando carpeta no existe")
    void should_ThrowException_When_CarpetaNotFound() {
        // Arrange
        Long carpetaId = new Random().nextLong();
        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            carpetaService.obtenerPorId(carpetaId, organizacionId)
        )
        .isInstanceOf(CarpetaNotFoundException.class);
    }
    
    @Test
    @DisplayName("Debería listar carpetas hijas correctamente")
    void should_ListChildren_When_ParentExists() {
        // Arrange
        List<Carpeta> hijasEsperadas = List.of(
                Carpeta.builder()
                        .id(new Random().nextLong())
                        .nombre("Hijo 1")
                        .carpetaPadreId(carpetaPadreId)
                        .organizacionId(organizacionId)
                        .creadoPor(usuarioId)
                        .fechaCreacion(Instant.now())
                        .fechaActualizacion(Instant.now())
                        .build(),
                Carpeta.builder()
                        .id(new Random().nextLong())
                        .nombre("Hijo 2")
                        .carpetaPadreId(carpetaPadreId)
                        .organizacionId(organizacionId)
                        .creadoPor(usuarioId)
                        .fechaCreacion(Instant.now())
                        .fechaActualizacion(Instant.now())
                        .build()
        );
        
        when(carpetaRepository.obtenerHijos(carpetaPadreId, organizacionId))
                .thenReturn(hijasEsperadas);
        
        // Act
        List<Carpeta> resultado = carpetaService.obtenerHijos(carpetaPadreId, organizacionId);
        
        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(Carpeta::getNombre)
                .containsExactly("Hijo 1", "Hijo 2");
    }
    
    @Test
    @DisplayName("Debería obtener carpeta raíz correctamente")
    void should_GetRootFolder_When_Exists() {
        // Arrange
        Carpeta raizEsperada = Carpeta.builder()
                .id(new Random().nextLong())
                .nombre("Raíz")
                .carpetaPadreId(null)  // Sin padre = raíz
                .organizacionId(organizacionId)
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
        
        when(carpetaRepository.obtenerRaiz(organizacionId))
                .thenReturn(Optional.of(raizEsperada));
        
        // Act
        Carpeta resultado = carpetaService.obtenerRaiz(organizacionId);
        
        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.esRaiz()).isTrue();
        assertThat(resultado.getCarpetaPadreId()).isNull();
    }
    
    // ========================================================================
    // TESTS DE ELIMINACIÓN
    // ========================================================================
    
    @Test
    @DisplayName("Debería eliminar carpeta lógicamente")
    void should_DeleteCarpetaLogically_When_Exists() {
        // Arrange
        Long carpetaId = new Random().nextLong();
        when(carpetaRepository.eliminarLogicamente(carpetaId, organizacionId))
                .thenReturn(true);
        
        // Act
        boolean resultado = carpetaService.eliminar(carpetaId, organizacionId);
        
        // Assert
        assertThat(resultado).isTrue();
        verify(carpetaRepository).eliminarLogicamente(carpetaId, organizacionId);
    }
    
    @Test
    @DisplayName("Debería lanzar excepción al eliminar carpeta que no existe")
    void should_ThrowException_When_DeletingNonExistentCarpeta() {
        // Arrange
        Long carpetaId = new Random().nextLong();
        when(carpetaRepository.eliminarLogicamente(carpetaId, organizacionId))
                .thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> 
            carpetaService.eliminar(carpetaId, organizacionId)
        )
        .isInstanceOf(CarpetaNotFoundException.class);
    }
    
    // ========================================================================
    // TESTS DE EVENTOS
    // ========================================================================
    
    @Test
    @DisplayName("Debería emitir evento CarpetaCreatedEvent al crear carpeta")
    void should_EmitEvent_When_CarpetaCreated() {
        // Arrange
        String nombre = "Nueva Carpeta";
        Carpeta carpetaCreada = Carpeta.builder()
                .id(new Random().nextLong())
                .nombre(nombre)
                .carpetaPadreId(carpetaPadreId)
                .organizacionId(organizacionId)
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
        
        when(carpetaRepository.crear(any(Carpeta.class))).thenReturn(carpetaCreada);
        
        // Act
        carpetaService.crear(nombre, null, carpetaPadreId, organizacionId, usuarioId);
        
        // Assert
        ArgumentCaptor<CarpetaCreatedEvent> eventCaptor = 
                ArgumentCaptor.forClass(CarpetaCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        CarpetaCreatedEvent eventoCapturado = eventCaptor.getValue();
        assertThat(eventoCapturado).isNotNull();
        assertThat(eventoCapturado.carpetaId()).isEqualTo(carpetaCreada.getId());
        assertThat(eventoCapturado.organizacionId()).isEqualTo(organizacionId);
        assertThat(eventoCapturado.usuarioId()).isEqualTo(usuarioId);
        assertThat(eventoCapturado.nombre()).isEqualTo(nombre);
    }
}
