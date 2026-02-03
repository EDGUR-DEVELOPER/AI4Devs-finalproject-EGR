package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.CarpetaAncestro;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
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
 */
@Service
@Transactional(readOnly = true)
public class PermisoHerenciaService {

    private static final Logger logger = LoggerFactory.getLogger(PermisoHerenciaService.class);

    private final IPermisoCarpetaUsuarioRepository permisoRepository;
    private final ICarpetaRepository carpetaRepository;

    public PermisoHerenciaService(
            IPermisoCarpetaUsuarioRepository permisoRepository,
            ICarpetaRepository carpetaRepository
    ) {
        this.permisoRepository = permisoRepository;
        this.carpetaRepository = carpetaRepository;
    }

    /**
     * Evalúa el permiso efectivo de un usuario sobre una carpeta.
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

        Optional<PermisoCarpetaUsuario> permisoDirecto = permisoRepository
                .findByCarpetaIdAndUsuarioIdAndOrganizacionId(carpetaId, usuarioId, organizacionId);

        if (permisoDirecto.isPresent()) {
            PermisoCarpetaUsuario permiso = permisoDirecto.get();
            return Optional.of(PermisoEfectivo.directo(
                    permiso.getNivelAcceso(),
                    carpetaId,
                    carpeta.getNombre()
            ));
        }

        List<CarpetaAncestro> rutaAncestros = carpetaRepository.obtenerRutaAncestros(
                carpetaId,
                organizacionId
        );

        if (rutaAncestros.isEmpty()) {
            return Optional.empty();
        }

        List<Long> ancestroIds = rutaAncestros.stream()
                .map(CarpetaAncestro::getId)
                .toList();

        List<PermisoCarpetaUsuario> permisosAncestros = permisoRepository
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
                        carpeta.getNombre()
                );

                return Optional.of(PermisoEfectivo.heredado(
                        permisoAncestro.getNivelAcceso(),
                        ancestro.getId(),
                        ancestro.getNombre(),
                        rutaHerencia
                ));
            }

            logger.debug("ACL no recursivo encontrado en carpeta {}, se detiene la búsqueda",
                    ancestro.getId());
            return Optional.empty();
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
