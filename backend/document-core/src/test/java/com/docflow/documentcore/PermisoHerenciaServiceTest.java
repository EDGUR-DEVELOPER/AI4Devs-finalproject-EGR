package com.docflow.documentcore;

import com.docflow.documentcore.application.service.PermisoHerenciaService;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.CarpetaAncestro;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisoHerenciaService - Tests Unitarios")
class PermisoHerenciaServiceTest {

    @Mock
    private IPermisoCarpetaUsuarioRepository permisoRepository;

    @Mock
    private ICarpetaRepository carpetaRepository;

    @InjectMocks
    private PermisoHerenciaService service;

    private Long usuarioId;
    private Long organizacionId;
    private Long carpetaId;

    @BeforeEach
    void setUp() {
        usuarioId = Math.abs(new Random().nextLong());
        organizacionId = Math.abs(new Random().nextLong());
        carpetaId = Math.abs(new Random().nextLong());
    }

    @Test
    @DisplayName("Debería retornar permiso directo cuando existe ACL directo")
    void should_ReturnDirectPermission_When_DirectAclExists() {
        Carpeta carpeta = buildCarpeta(carpetaId, "Raiz", organizacionId);
        PermisoCarpetaUsuario permiso = buildPermiso(carpetaId, NivelAcceso.LECTURA, false, organizacionId);

        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId)).thenReturn(Optional.of(carpeta));
        when(permisoRepository.findByCarpetaIdAndUsuarioIdAndOrganizacionId(
                carpetaId, usuarioId, organizacionId)).thenReturn(Optional.of(permiso));

        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
                usuarioId, carpetaId, organizacionId
        );

        assertThat(result).isPresent();
        assertThat(result.get().isEsHeredado()).isFalse();
        assertThat(result.get().getCarpetaOrigenId()).isEqualTo(carpetaId);
        assertThat(result.get().getRutaHerencia()).isNull();

        verify(permisoRepository, never()).findByUsuarioIdAndCarpetaIds(any(), any(), any());
    }

    @Test
    @DisplayName("Debería heredar permiso del ancestro más cercano con recursivo")
    void should_ReturnInheritedPermission_When_RecursiveAclExists() {
        Long padreId = Math.abs(new Random().nextLong());
        Carpeta carpeta = buildCarpeta(carpetaId, "Q1", organizacionId);
        CarpetaAncestro padre = new CarpetaAncestro(padreId, "Proyectos", 1);
        PermisoCarpetaUsuario permisoPadre = buildPermiso(padreId, NivelAcceso.ESCRITURA, true, organizacionId);

        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId)).thenReturn(Optional.of(carpeta));
        when(permisoRepository.findByCarpetaIdAndUsuarioIdAndOrganizacionId(
                carpetaId, usuarioId, organizacionId)).thenReturn(Optional.empty());
        when(carpetaRepository.obtenerRutaAncestros(carpetaId, organizacionId))
                .thenReturn(List.of(padre));
        when(permisoRepository.findByUsuarioIdAndCarpetaIds(
                usuarioId, List.of(padreId), organizacionId))
                .thenReturn(List.of(permisoPadre));

        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
                usuarioId, carpetaId, organizacionId
        );

        assertThat(result).isPresent();
        assertThat(result.get().isEsHeredado()).isTrue();
        assertThat(result.get().getCarpetaOrigenId()).isEqualTo(padreId);
        assertThat(result.get().getRutaHerencia()).containsExactly("Proyectos", "Q1");
    }

    @Test
    @DisplayName("Debería detener herencia cuando encuentra ACL no recursivo")
    void should_StopInheritance_When_NonRecursiveAclFound() {
        Long padreId = Math.abs(new Random().nextLong());
        Carpeta carpeta = buildCarpeta(carpetaId, "Q1", organizacionId);
        CarpetaAncestro padre = new CarpetaAncestro(padreId, "Proyectos", 1);
        PermisoCarpetaUsuario permisoPadre = buildPermiso(padreId, NivelAcceso.LECTURA, false, organizacionId);

        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId)).thenReturn(Optional.of(carpeta));
        when(permisoRepository.findByCarpetaIdAndUsuarioIdAndOrganizacionId(
                carpetaId, usuarioId, organizacionId)).thenReturn(Optional.empty());
        when(carpetaRepository.obtenerRutaAncestros(carpetaId, organizacionId))
                .thenReturn(List.of(padre));
        when(permisoRepository.findByUsuarioIdAndCarpetaIds(
                usuarioId, List.of(padreId), organizacionId))
                .thenReturn(List.of(permisoPadre));

        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
                usuarioId, carpetaId, organizacionId
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debería retornar vacío cuando no hay permisos heredados")
    void should_ReturnEmpty_When_NoInheritedPermissions() {
        Carpeta carpeta = buildCarpeta(carpetaId, "Q1", organizacionId);

        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId)).thenReturn(Optional.of(carpeta));
        when(permisoRepository.findByCarpetaIdAndUsuarioIdAndOrganizacionId(
                carpetaId, usuarioId, organizacionId)).thenReturn(Optional.empty());
        when(carpetaRepository.obtenerRutaAncestros(carpetaId, organizacionId)).thenReturn(List.of());

        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
                usuarioId, carpetaId, organizacionId
        );

        assertThat(result).isEmpty();
    }

    private Carpeta buildCarpeta(Long id, String nombre, Long orgId) {
        return Carpeta.builder()
                .id(id)
                .nombre(nombre)
                .organizacionId(orgId)
                .creadoPor(1L)
                .build();
    }

    private PermisoCarpetaUsuario buildPermiso(
            Long carpetaId,
            NivelAcceso nivelAcceso,
            boolean recursivo,
            Long orgId
    ) {
        PermisoCarpetaUsuario permiso = new PermisoCarpetaUsuario();
        permiso.setCarpetaId(carpetaId);
        permiso.setUsuarioId(usuarioId);
        permiso.setOrganizacionId(orgId);
        permiso.setNivelAcceso(nivelAcceso);
        permiso.setRecursivo(recursivo);
        return permiso;
    }
}
