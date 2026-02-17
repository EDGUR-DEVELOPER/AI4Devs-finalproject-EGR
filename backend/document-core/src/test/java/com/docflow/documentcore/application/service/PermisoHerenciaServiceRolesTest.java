package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoCarpetaRol;
import com.docflow.documentcore.domain.model.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaRolRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.infrastructure.adapter.persistence.UsuarioRolesAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisoHerenciaService - Evaluación de permisos con roles")
class PermisoHerenciaServiceRolesTest {

    @Mock
    private IPermisoCarpetaUsuarioRepository permisoUsuarioRepository;

    @Mock
    private IPermisoCarpetaRolRepository permisoRolRepository;

    @Mock
    private ICarpetaRepository carpetaRepository;

    @Mock
    private UsuarioRolesAdapter usuarioRolesAdapter;

    private PermisoHerenciaService service;

    private static final Long USUARIO_ID = 1L;
    private static final Long CARPETA_ID = 100L;
    private static final Long ORGANIZACION_ID = 1L;
    private static final Long ROL_ID = 10L;
    private static final Long CARPETA_PADRE_ID = 99L;

    @BeforeEach
    void setUp() {
        service = new PermisoHerenciaService(
                permisoUsuarioRepository,
                permisoRolRepository,
                carpetaRepository,
                usuarioRolesAdapter
        );
    }

    @Test
    @DisplayName("Debe retornar permiso directo de usuario si existe")
    void testPermisoDirectoUsuario() {
        // Arrange
        Carpeta carpeta = Carpeta.builder()
                .id(CARPETA_ID)
                .nombre("Documentos")
                .organizacionId(ORGANIZACION_ID)
                .creadoPor(USUARIO_ID)
                .build();

        PermisoCarpetaUsuario permisoDirecto = new PermisoCarpetaUsuario();
        permisoDirecto.setCarpetaId(CARPETA_ID);
        permisoDirecto.setUsuarioId(USUARIO_ID);
        permisoDirecto.setNivelAcceso(NivelAcceso.ADMINISTRACION);
        permisoDirecto.setOrganizacionId(ORGANIZACION_ID);
        permisoDirecto.setRecursivo(true);

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        when(permisoUsuarioRepository.findByCarpetaIdAndUsuarioIdAndOrganizacionId(
                CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(permisoDirecto));

        // Act
        Optional<PermisoEfectivo> resultado = service.evaluarPermisoEfectivo(
                USUARIO_ID,
                CARPETA_ID,
                ORGANIZACION_ID
        );

        // Assert  
        assertTrue(resultado.isPresent());
        assertEquals(NivelAcceso.ADMINISTRACION, resultado.get().getNivelAcceso());
    }

    @Test
    @DisplayName("Debe retornar permiso por rol si usuario no tiene permiso directo")
    void testPermisoDirectoPorRol() {
        // Arrange
        Carpeta carpeta = Carpeta.builder()
                .id(CARPETA_ID)
                .nombre("Documentos")
                .organizacionId(ORGANIZACION_ID)
                .creadoPor(USUARIO_ID)
                .build();

        PermisoCarpetaRol permisoRol = new PermisoCarpetaRol();
        permisoRol.setCarpetaId(CARPETA_ID);
        permisoRol.setRolId(ROL_ID);
        permisoRol.setNivelAcceso(NivelAcceso.ESCRITURA);
        permisoRol.setOrganizacionId(ORGANIZACION_ID);
        permisoRol.setRecursivo(true);

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        when(permisoUsuarioRepository.findByCarpetaIdAndUsuarioIdAndOrganizacionId(
                CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty());
        when(usuarioRolesAdapter.obtenerRolesDelUsuario(USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(List.of(ROL_ID));
        when(permisoRolRepository.findByCarpetaIdAndRolIdInAndOrganizacionId(
                CARPETA_ID, List.of(ROL_ID), ORGANIZACION_ID))
                .thenReturn(List.of(permisoRol));

        // Act
        Optional<PermisoEfectivo> resultado = service.evaluarPermisoEfectivo(
                USUARIO_ID,
                CARPETA_ID,
                ORGANIZACION_ID
        );

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals(NivelAcceso.ESCRITURA, resultado.get().getNivelAcceso());
    }

    @Test
    @DisplayName("Debe retornar vacío si no hay permisos directos ni heredados")
    void testSinPermisos() {
        // Arrange
        Carpeta carpeta = Carpeta.builder()
                .id(CARPETA_ID)
                .nombre("Documentos")
                .organizacionId(ORGANIZACION_ID)
                .creadoPor(USUARIO_ID)
                .build();

        when(carpetaRepository.obtenerPorId(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(Optional.of(carpeta));
        when(permisoUsuarioRepository.findByCarpetaIdAndUsuarioIdAndOrganizacionId(
                CARPETA_ID, USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(Optional.empty());
        when(usuarioRolesAdapter.obtenerRolesDelUsuario(USUARIO_ID, ORGANIZACION_ID))
                .thenReturn(List.of());
        when(carpetaRepository.obtenerRutaAncestros(CARPETA_ID, ORGANIZACION_ID))
                .thenReturn(List.of());

        // Act
        Optional<PermisoEfectivo> resultado = service.evaluarPermisoEfectivo(
                USUARIO_ID,
                CARPETA_ID,
                ORGANIZACION_ID
        );

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
