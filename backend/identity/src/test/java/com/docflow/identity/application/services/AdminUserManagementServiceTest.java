package com.docflow.identity.application.services;

import com.docflow.identity.application.dto.ListUsersResponseDto;
import com.docflow.identity.application.ports.UserWithRolesProjection;
import com.docflow.identity.application.ports.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para AdminUserManagementService.listUsers().
 * 
 * Cubre escenarios:
 * 1. Listado exitoso con usuarios y roles agrupados
 * 2. Listado vacío (sin usuarios en la organización)
 * 3. Filtro por estado SUSPENDIDO
 * 4. Búsqueda parcial por email
 * 5. Búsqueda parcial por nombre
 * 6. Metadata de paginación recalculada después de filtros
 * 7. Límite forzado a 100 cuando se pasa valor mayor
 */
@SpringBootTest
@DisplayName("AdminUserManagementService - listUsers()")
class AdminUserManagementServiceTest {
    
    @Autowired
    private AdminUserManagementService adminUserService;
    
    @MockBean
    private UsuarioRepository usuarioRepository;
    
    private static final Integer ORG_ID = 1;
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    
    @BeforeEach
    void setUp() {
        // Setup común si es necesario
    }
    
    @Test
    @DisplayName("Debe retornar lista de usuarios con roles agrupados correctamente")
    void testListUsersSuccess() {
        // Arrange: Simular 2 usuarios, uno con 2 roles, otro con 1 rol
        var proyeccion1Usuario1 = new UserWithRolesProjection(
            101L, "admin@test.com", "Admin User", "ACTIVO",
            1, "ADMIN", "Administrador", NOW
        );
        var proyeccion2Usuario1 = new UserWithRolesProjection(
            101L, "admin@test.com", "Admin User", "ACTIVO",
            3, "OPERATOR", "Operador", NOW
        );
        var proyeccion1Usuario2 = new UserWithRolesProjection(
            102L, "user@test.com", "Regular User", "ACTIVO",
            2, "USER", "Usuario", NOW
        );
        
        Page<UserWithRolesProjection> page = new PageImpl<>(
            List.of(proyeccion1Usuario1, proyeccion2Usuario1, proyeccion1Usuario2),
            PageRequest.of(0, 20),
            3
        );
        
        when(usuarioRepository.findUsersWithRolesByOrganizacion(eq(ORG_ID), any(Pageable.class)))
            .thenReturn(page);
        
        // Act
        ListUsersResponseDto response = adminUserService.listUsers(
            ORG_ID, 1, 20, Optional.empty(), Optional.empty()
        );
        
        // Assert
        assertThat(response.usuarios()).hasSize(2); // 2 usuarios únicos
        assertThat(response.usuarios().get(0).roles()).hasSize(2); // Usuario 1 con 2 roles
        assertThat(response.usuarios().get(1).roles()).hasSize(1); // Usuario 2 con 1 rol
        
        assertThat(response.usuarios().get(0).id()).isEqualTo(101L);
        assertThat(response.usuarios().get(0).email()).isEqualTo("admin@test.com");
        
        assertThat(response.paginacion().total()).isEqualTo(2);
        assertThat(response.paginacion().pagina()).isEqualTo(1);
        assertThat(response.paginacion().limite()).isEqualTo(20);
    }
    
    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay usuarios en la organización")
    void testListUsersEmpty() {
        // Arrange
        Page<UserWithRolesProjection> emptyPage = new PageImpl<>(
            List.of(),
            PageRequest.of(0, 20),
            0
        );
        
        when(usuarioRepository.findUsersWithRolesByOrganizacion(eq(ORG_ID), any(Pageable.class)))
            .thenReturn(emptyPage);
        
        // Act
        ListUsersResponseDto response = adminUserService.listUsers(
            ORG_ID, 1, 20, Optional.empty(), Optional.empty()
        );
        
        // Assert
        assertThat(response.usuarios()).isEmpty();
        assertThat(response.paginacion().total()).isZero();
        assertThat(response.paginacion().totalPaginas()).isEqualTo(1); // Mínimo 1 página
    }
    
    @Test
    @DisplayName("Debe filtrar solo usuarios con estado SUSPENDIDO")
    void testFilterByEstadoSuspendido() {
        // Arrange: 1 usuario ACTIVO, 1 SUSPENDIDO
        var proyeccionActivo = new UserWithRolesProjection(
            101L, "activo@test.com", "User Activo", "ACTIVO",
            2, "USER", "Usuario", NOW
        );
        var proyeccionSuspendido = new UserWithRolesProjection(
            104L, "suspendido@test.com", "User Suspendido", "SUSPENDIDO",
            2, "USER", "Usuario", NOW
        );
        
        Page<UserWithRolesProjection> page = new PageImpl<>(
            List.of(proyeccionActivo, proyeccionSuspendido),
            PageRequest.of(0, 20),
            2
        );
        
        when(usuarioRepository.findUsersWithRolesByOrganizacion(eq(ORG_ID), any(Pageable.class)))
            .thenReturn(page);
        
        // Act
        ListUsersResponseDto response = adminUserService.listUsers(
            ORG_ID, 1, 20, Optional.of("SUSPENDIDO"), Optional.empty()
        );
        
        // Assert
        assertThat(response.usuarios()).hasSize(1);
        assertThat(response.usuarios().get(0).estado()).isEqualTo("SUSPENDIDO");
        assertThat(response.usuarios().get(0).email()).isEqualTo("suspendido@test.com");
    }
    
    @Test
    @DisplayName("Debe buscar usuarios por email parcial (case-insensitive)")
    void testSearchByEmailPartial() {
        // Arrange
        var proyeccion1 = new UserWithRolesProjection(
            105L, "TEST.admin@acme.com", "Test Admin", "ACTIVO",
            1, "ADMIN", "Administrador", NOW
        );
        var proyeccion2 = new UserWithRolesProjection(
            106L, "user@acme.com", "Regular User", "ACTIVO",
            2, "USER", "Usuario", NOW
        );
        
        Page<UserWithRolesProjection> page = new PageImpl<>(
            List.of(proyeccion1, proyeccion2),
            PageRequest.of(0, 20),
            2
        );
        
        when(usuarioRepository.findUsersWithRolesByOrganizacion(eq(ORG_ID), any(Pageable.class)))
            .thenReturn(page);
        
        // Act: Buscar "test" en email (case-insensitive)
        ListUsersResponseDto response = adminUserService.listUsers(
            ORG_ID, 1, 20, Optional.empty(), Optional.of("test")
        );
        
        // Assert: Solo debe retornar el usuario con "test" en email
        assertThat(response.usuarios()).hasSize(1);
        assertThat(response.usuarios().get(0).email()).containsIgnoringCase("test");
    }
    
    @Test
    @DisplayName("Debe buscar usuarios por nombre parcial (case-insensitive)")
    void testSearchByNombrePartial() {
        // Arrange
        var proyeccion1 = new UserWithRolesProjection(
            106L, "juan.perez@acme.com", "Juan Pérez González", "ACTIVO",
            2, "USER", "Usuario", NOW
        );
        var proyeccion2 = new UserWithRolesProjection(
            107L, "maria@acme.com", "María García", "ACTIVO",
            2, "USER", "Usuario", NOW
        );
        
        Page<UserWithRolesProjection> page = new PageImpl<>(
            List.of(proyeccion1, proyeccion2),
            PageRequest.of(0, 20),
            2
        );
        
        when(usuarioRepository.findUsersWithRolesByOrganizacion(eq(ORG_ID), any(Pageable.class)))
            .thenReturn(page);
        
        // Act: Buscar "juan" en nombre (case-insensitive)
        ListUsersResponseDto response = adminUserService.listUsers(
            ORG_ID, 1, 20, Optional.empty(), Optional.of("juan")
        );
        
        // Assert: Solo debe retornar el usuario con "juan" en nombre
        assertThat(response.usuarios()).hasSize(1);
        assertThat(response.usuarios().get(0).nombreCompleto()).containsIgnoringCase("juan");
    }
    
    @Test
    @DisplayName("Debe recalcular metadata de paginación después de aplicar filtros")
    void testPaginationMetadataRecalculatedAfterFilters() {
        // Arrange: 10 usuarios en BD, pero solo 3 con estado ACTIVO
        var proyecciones = List.of(
            new UserWithRolesProjection(101L, "u1@test.com", "U1", "ACTIVO", 2, "USER", "Usuario", NOW),
            new UserWithRolesProjection(102L, "u2@test.com", "U2", "ACTIVO", 2, "USER", "Usuario", NOW),
            new UserWithRolesProjection(103L, "u3@test.com", "U3", "ACTIVO", 2, "USER", "Usuario", NOW),
            new UserWithRolesProjection(104L, "u4@test.com", "U4", "SUSPENDIDO", 2, "USER", "Usuario", NOW),
            new UserWithRolesProjection(105L, "u5@test.com", "U5", "SUSPENDIDO", 2, "USER", "Usuario", NOW)
        );
        
        Page<UserWithRolesProjection> page = new PageImpl<>(
            proyecciones,
            PageRequest.of(0, 5),
            10 // Total en BD
        );
        
        when(usuarioRepository.findUsersWithRolesByOrganizacion(eq(ORG_ID), any(Pageable.class)))
            .thenReturn(page);
        
        // Act: Filtrar solo ACTIVO con limit=5
        ListUsersResponseDto response = adminUserService.listUsers(
            ORG_ID, 1, 5, Optional.of("ACTIVO"), Optional.empty()
        );
        
        // Assert: Metadata debe reflejar 3 usuarios filtrados, no 10 totales
        assertThat(response.usuarios()).hasSize(3);
        assertThat(response.paginacion().total()).isEqualTo(3); // Solo los ACTIVO
        assertThat(response.paginacion().totalPaginas()).isEqualTo(1); // 3 usuarios caben en 1 página de 5
    }
    
    @Test
    @DisplayName("Debe forzar límite a 100 cuando se pasa valor mayor")
    void testLimitForcedTo100() {
        // Arrange
        var proyeccion = new UserWithRolesProjection(
            101L, "user@test.com", "User", "ACTIVO",
            2, "USER", "Usuario", NOW
        );
        
        Page<UserWithRolesProjection> page = new PageImpl<>(
            List.of(proyeccion),
            PageRequest.of(0, 100), // El servicio debe ajustar a 100
            1
        );
        
        when(usuarioRepository.findUsersWithRolesByOrganizacion(eq(ORG_ID), any(Pageable.class)))
            .thenReturn(page);
        
        // Act: Intentar pasar limit=200 (debe forzarse a 100)
        ListUsersResponseDto response = adminUserService.listUsers(
            ORG_ID, 1, 200, Optional.empty(), Optional.empty()
        );
        
        // Assert: El límite en metadata debe ser 100, no 200
        assertThat(response.paginacion().limite()).isEqualTo(100);
    }
}
