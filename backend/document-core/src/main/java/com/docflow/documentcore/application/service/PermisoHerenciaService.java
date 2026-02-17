package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.CarpetaAncestro;
import com.docflow.documentcore.domain.model.PermisoCarpetaRol;
import com.docflow.documentcore.domain.model.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaRolRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.infrastructure.adapter.persistence.UsuarioRolesAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para resolver permisos heredados en carpetas.
 * 
 * Implementa evaluación de permisos combinando:
 * 1. Permisos directos usuario-carpeta (permiso_carpeta_usuario)
 * 2. Permisos por rol (permiso_carpeta_rol) heredados a través de usuarios_roles
 * 
 * Precedencia: Permiso directo usuario > Permiso por rol > Herencia en carpetas
 */
@Service
@Transactional(readOnly = true)
public class PermisoHerenciaService {

    private static final Logger logger = LoggerFactory.getLogger(PermisoHerenciaService.class);

    private final IPermisoCarpetaUsuarioRepository permisoUsuarioRepository;
    private final IPermisoCarpetaRolRepository permisoRolRepository;
    private final ICarpetaRepository carpetaRepository;
    private final UsuarioRolesAdapter usuarioRolesAdapter;

    public PermisoHerenciaService(
            IPermisoCarpetaUsuarioRepository permisoUsuarioRepository,
            IPermisoCarpetaRolRepository permisoRolRepository,
            ICarpetaRepository carpetaRepository,
            UsuarioRolesAdapter usuarioRolesAdapter
    ) {
        this.permisoUsuarioRepository = permisoUsuarioRepository;
        this.permisoRolRepository = permisoRolRepository;
        this.carpetaRepository = carpetaRepository;
        this.usuarioRolesAdapter = usuarioRolesAdapter;
    }

    /**
     * Evalúa el permiso efectivo de un usuario sobre una carpeta.
     * 
     * Precedencia de evaluación:
     * 1. Permiso directo usuario-carpeta (permiso_carpeta_usuario)
     * 2. Permiso por rol en la carpeta (permiso_carpeta_rol)
     * 3. Permisos heredados en ancestros (directo o por rol)
     *
     * @param usuarioId usuario autenticado
     * @param carpetaId carpeta objetivo
     * @param organizacionId organización del usuario
     * @return permiso efectivo (directo o heredado) o vacío si no hay permiso
     */
    public Optional<PermisoEfectivo> evaluarPermisoEfectivo(
            Long usuarioId,
            Long carpetaId,
            Long organizacionId
    ) {
        logger.debug("Evaluando permiso efectivo: usuario={}, carpeta={}, org={}",
                usuarioId, carpetaId, organizacionId);

        Carpeta carpeta = carpetaRepository.obtenerPorId(carpetaId, organizacionId)
                .orElseThrow(() -> new CarpetaNotFoundException(carpetaId));

        // PASO 1: Buscar permiso DIRECTO usuario-carpeta
        Optional<PermisoCarpetaUsuario> permisoDirectoUsuario = permisoUsuarioRepository
                .findByCarpetaIdAndUsuarioIdAndOrganizacionId(carpetaId, usuarioId, organizacionId);

        if (permisoDirectoUsuario.isPresent()) {
            PermisoCarpetaUsuario permiso = permisoDirectoUsuario.get();
            logger.debug("Permiso directo de usuario encontrado: nivel={}", permiso.getNivelAcceso());
            return Optional.of(PermisoEfectivo.carpetaDirecto(
                    permiso.getNivelAcceso(),
                    carpetaId,
                    carpeta.getNombre()
            ));
        }

        // PASO 2: Obtener roles del usuario y buscar permisos por rol
        List<Long> rolesDelUsuario = usuarioRolesAdapter.obtenerRolesDelUsuario(usuarioId, organizacionId);
        
        if (!rolesDelUsuario.isEmpty()) {
            // Buscar permisos de rol directos en la carpeta
            Optional<PermisoCarpetaRol> permisoDirectoRol = evaluarPermisoDirectoPorRol(
                    carpetaId,
                    rolesDelUsuario,
                    organizacionId
            );
            
            if (permisoDirectoRol.isPresent()) {
                logger.debug("Permiso directo por rol encontrado: nivel={}", 
                        permisoDirectoRol.get().getNivelAcceso());
                return Optional.of(PermisoEfectivo.carpetaDirecto(
                        permisoDirectoRol.get().getNivelAcceso(),
                        carpetaId,
                        carpeta.getNombre()
                ));
            }
        }

        // PASO 3: Buscar permisos heredados en ancestros (usuario y rol)
        List<CarpetaAncestro> rutaAncestros = carpetaRepository.obtenerRutaAncestros(
                carpetaId,
                organizacionId
        );

        if (rutaAncestros.isEmpty()) {
            logger.debug("No hay ancestros para la carpeta: carpetaId={}", carpetaId);
            return Optional.empty();
        }

        // Buscar permisos heredados del usuario
        Optional<PermisoEfectivo> permisoHeredadoUsuario = evaluarPermisosHeredadosUsuario(
                usuarioId,
                carpetaId,
                carpeta.getNombre(),
                rutaAncestros,
                organizacionId
        );

        if (permisoHeredadoUsuario.isPresent()) {
            return permisoHeredadoUsuario;
        }

        // Buscar permisos heredados por rol
        if (!rolesDelUsuario.isEmpty()) {
            Optional<PermisoEfectivo> permisoHeredadoRol = evaluarPermisosHeredadosPorRol(
                    rolesDelUsuario,
                    carpetaId,
                    carpeta.getNombre(),
                    rutaAncestros,
                    organizacionId
            );

            if (permisoHeredadoRol.isPresent()) {
                return permisoHeredadoRol;
            }
        }

        logger.debug("No se encontró permiso efectivo para usuario={} en carpeta={}", 
                usuarioId, carpetaId);
        return Optional.empty();
    }

    /**
     * Evalúa si el usuario tiene permisos por rol directos en la carpeta.
     * Retorna el permiso de mayor jerarquía si tiene múltiples roles.
     */
    private Optional<PermisoCarpetaRol> evaluarPermisoDirectoPorRol(
            Long carpetaId,
            List<Long> rolesIds,
            Long organizacionId
    ) {
        List<PermisoCarpetaRol> permisosRol = permisoRolRepository
                .findByCarpetaIdAndRolIdInAndOrganizacionId(carpetaId, rolesIds, organizacionId);

        if (permisosRol.isEmpty()) {
            return Optional.empty();
        }

        // Retornar el permiso de mayor nivel (ADMINISTRACION > ESCRITURA > LECTURA)
        return permisosRol.stream()
                .max(Comparator.comparing(p -> p.getNivelAcceso().ordinal()));
    }

    /**
     * Evalúa permisos heredados del usuario en la jerarquía de carpetas.
     */
    private Optional<PermisoEfectivo> evaluarPermisosHeredadosUsuario(
            Long usuarioId,
            Long carpetaId,
            String carpetaNombre,
            List<CarpetaAncestro> rutaAncestros,
            Long organizacionId
    ) {
        List<Long> ancestroIds = rutaAncestros.stream()
                .map(CarpetaAncestro::getId)
                .toList();

        List<PermisoCarpetaUsuario> permisosAncestros = permisoUsuarioRepository
                .findByUsuarioIdAndCarpetaIds(usuarioId, ancestroIds, organizacionId);

        Map<Long, PermisoCarpetaUsuario> permisosPorCarpeta = permisosAncestros.stream()
                .collect(Collectors.toMap(
                        PermisoCarpetaUsuario::getCarpetaId,
                        Function.identity(),
                        (actual, reemplazo) -> actual
                ));

        for (CarpetaAncestro ancestro : rutaAncestros) {
            PermisoCarpetaUsuario permisoAncestro = permisosPorCarpeta.get(ancestro.getId());

            if (permisoAncestro == null) {
                continue;
            }

            if (Boolean.TRUE.equals(permisoAncestro.getRecursivo())) {
                List<String> rutaHerencia = construirRutaHerencia(
                        rutaAncestros,
                        ancestro,
                        carpetaNombre
                );

                logger.debug("Permiso heredado por usuario encontrado: ancestro={}, nivel={}", 
                        ancestro.getId(), permisoAncestro.getNivelAcceso());

                return Optional.of(PermisoEfectivo.carpetaHeredado(
                        permisoAncestro.getNivelAcceso(),
                        ancestro.getId(),
                        ancestro.getNombre(),
                        rutaHerencia
                ));
            }

            logger.debug("Permiso no recursivo encontrado en carpeta {}, deteniendo búsqueda",
                    ancestro.getId());
            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * Evalúa permisos heredados a través de roles en la jerarquía de carpetas.
     */
    private Optional<PermisoEfectivo> evaluarPermisosHeredadosPorRol(
            List<Long> rolesIds,
            Long carpetaId,
            String carpetaNombre,
            List<CarpetaAncestro> rutaAncestros,
            Long organizacionId
    ) {
        rutaAncestros.stream()
                .map(CarpetaAncestro::getId)
                .toList();

        // Buscar permisos de roles heredables en ancestros
        List<PermisoCarpetaRol> permisosRolAncestros = permisoRolRepository
                .findByCarpetaIdAndRolIdInAndOrganizacionId(carpetaId, rolesIds, organizacionId)
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getRecursivo()))
                .toList();

        if (!permisosRolAncestros.isEmpty()) {
            // Retornar el permiso de mayor nivel
            PermisoCarpetaRol permisoMayor = permisosRolAncestros.stream()
                    .max(Comparator.comparing(p -> p.getNivelAcceso().ordinal()))
                    .orElse(null);

            if (permisoMayor != null) {
                logger.debug("Permiso heredado por rol encontrado: nivel={}", 
                        permisoMayor.getNivelAcceso());
                
                return Optional.of(PermisoEfectivo.carpetaDirecto(
                        permisoMayor.getNivelAcceso(),
                        carpetaId,
                        carpetaNombre
                ));
            }
        }

        return Optional.empty();
    }

    private List<String> construirRutaHerencia(
            List<CarpetaAncestro> ancestros,
            CarpetaAncestro origen,
            String carpetaDestinoNombre
    ) {
        List<String> ruta = new ArrayList<>();
        ruta.add(origen.getNombre());

        List<CarpetaAncestro> descendientes = ancestros.stream()
                .filter(ancestro -> ancestro.getNivel() < origen.getNivel())
                .sorted(Comparator.comparingInt(CarpetaAncestro::getNivel).reversed())
                .toList();

        for (CarpetaAncestro descendiente : descendientes) {
            ruta.add(descendiente.getNombre());
        }

        ruta.add(carpetaDestinoNombre);
        return ruta;
    }
}
