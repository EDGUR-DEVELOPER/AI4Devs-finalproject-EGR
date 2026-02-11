package com.docflow.documentcore.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.CarpetaRaizNoEncontradaException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.model.CapacidadesUsuario;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.CarpetaItem;
import com.docflow.documentcore.domain.model.ContenidoCarpeta;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.DocumentoItem;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.OpcionesListado;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.model.UsuarioResumen;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IDocumentoRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;

/**
 * Servicio de aplicación para listar el contenido de carpetas con filtrado de permisos.
 * 
 * <p>Responsabilidades:
 * <ul>
 *   <li>Validar acceso del usuario a la carpeta (permiso de lectura)</li>
 *   <li>Obtener subcarpetas y documentos accesibles</li>
 *   <li>Enriquecer resultados con permisos de cada item</li>
 *   <li>Aplicar paginación y ordenamiento</li>
 *   <li>Construir respuesta con totalización de contenido</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Service
@Transactional(readOnly = true)
public class CarpetaContenidoService {

    private final ICarpetaRepository carpetaRepository;
    private final IDocumentoRepository documentoRepository;
    private final IEvaluadorPermisos evaluadorPermisos;

    public CarpetaContenidoService(
            ICarpetaRepository carpetaRepository,
            IDocumentoRepository documentoRepository,
            IEvaluadorPermisos evaluadorPermisos) {
        this.carpetaRepository = carpetaRepository;
        this.documentoRepository = documentoRepository;
        this.evaluadorPermisos = evaluadorPermisos;
    }

    /**
     * Lista el contenido (subcarpetas y documentos) de una carpeta específica.
     * 
     * <p>Flujo de validación y obtención:
     * <ol>
     *   <li>Verificar que la carpeta existe y pertenece a la organización</li>
     *   <li>Evaluar permiso de lectura del usuario sobre la carpeta</li>
     *   <li>Si no tiene permiso, lanzar AccessDeniedException</li>
     *   <li>Obtener subcarpetas accesibles para el usuario</li>
     *   <li>Enriquecer cada subcarpeta con permisos específicos del usuario</li>
     *   <li>Obtener documentos de la carpeta con paginación</li>
     *   <li>Enriquecer cada documento con permisos específicos del usuario</li>
     *   <li>Contar totales de contenido accesible</li>
     *   <li>Construir y retornar respuesta con ContenidoCarpeta</li>
     * </ol>
     * </p>
     * 
     * @param carpetaId identificador de la carpeta a listar
     * @param usuarioId identificador del usuario ejecutando la operación
     * @param organizacionId identificador de la organización (tenant)
     * @param opciones opciones de paginación y ordenamiento
     * @return ContenidoCarpeta con subcarpetas, documentos y totalización
     * @throws CarpetaNotFoundException si la carpeta no existe
     * @throws AccessDeniedException si el usuario no tiene acceso de lectura
     */
    public ContenidoCarpeta obtenerContenidoCarpeta(
            Long carpetaId,
            Long usuarioId,
            Long organizacionId,
            OpcionesListado opciones) {

        // Step 1: Obtener la carpeta y validar existencia
        carpetaRepository.obtenerPorId(carpetaId, organizacionId)
                .orElseThrow(() -> new CarpetaNotFoundException(carpetaId));

        // Step 2: Evaluar permiso de lectura sobre la carpeta
        PermisoEfectivo permiso = evaluadorPermisos.evaluarPermisoCarpeta(
                usuarioId,
                carpetaId,
                organizacionId);

        if (permiso == null || !tienePermisoLectura(permiso.getNivelAcceso())) {
            throw new AccessDeniedException(
                    "Usuario sin acceso de lectura en carpeta",
                    "DOC-403");
        }

        // Step 3: Convertir OpcionesListado a Pageable para subcarpetas
        Pageable pageableSubcarpetas = convertirOpcionesAPageable(opciones);

        // Step 4: Obtener subcarpetas accesibles
        List<Carpeta> subcarpetasEntidades = carpetaRepository.obtenerSubcarpetasConPermiso(
                carpetaId,
                usuarioId,
                organizacionId);

        // Step 5: Enriquecer subcarpetas con permisos y convertir a CarpetaItem
        List<CarpetaItem> subcarpetas = subcarpetasEntidades.stream()
                .map(subcarpeta -> enriquecerCarpetaConPermisos(
                        subcarpeta,
                        usuarioId,
                        organizacionId))
                .collect(Collectors.toList());

        // Step 6: Obtener documentos de la carpeta con paginación
        List<Documento> documentosEntidades = documentoRepository.obtenerDocumentosConPermiso(
                carpetaId,
                usuarioId,
                organizacionId,
                pageableSubcarpetas);

        // Step 7: Enriquecer documentos con permisos y convertir a DocumentoItem
        List<DocumentoItem> documentos = documentosEntidades.stream()
                .map(documento -> enriquecerDocumentoConPermisos(
                        documento,
                        usuarioId,
                        organizacionId))
                .collect(Collectors.toList());

        // Step 8: Contar totales accesibles
        int totalSubcarpetas = carpetaRepository.contarSubcarpetasConPermiso(
                carpetaId,
                usuarioId,
                organizacionId);

        long totalDocumentos = documentoRepository.contarDocumentosConPermiso(
                carpetaId,
                usuarioId,
                organizacionId);

        // Step 9: Construir y retornar respuesta
        return new ContenidoCarpeta(
                subcarpetas,
                documentos,
                totalSubcarpetas,
                (int) totalDocumentos,
                opciones.getPagina(),
                calcularTotalPaginas((int) totalDocumentos, opciones.getTamanio()));
    }

    /**
     * Lista el contenido de la carpeta raíz de una organización.
     * 
     * <p>Delegación a obtenerContenidoCarpeta después de localizar la carpeta raíz.</p>
     * 
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @param opciones opciones de paginación y ordenamiento
     * @return ContenidoCarpeta con contenido de raíz
     * @throws CarpetaRaizNoEncontradaException si no existe carpeta raíz
     * @throws AccessDeniedException si el usuario no tiene acceso
     */
    public ContenidoCarpeta obtenerContenidoRaiz(
            Long usuarioId,
            Long organizacionId,
            OpcionesListado opciones) {

        // Localizar carpeta raíz
        Carpeta raiz = carpetaRepository.obtenerRaiz(organizacionId)
                .orElseThrow(() -> new CarpetaRaizNoEncontradaException(organizacionId));

        // Delegar a obtenerContenidoCarpeta
        return obtenerContenidoCarpeta(
                raiz.getId(),
                usuarioId,
                organizacionId,
                opciones);
    }

    // =================== Métodos Privados de Enriquecimiento ===================

    /**
     * Enriquece una carpeta con permisos específicos del usuario.
     */
    private CarpetaItem enriquecerCarpetaConPermisos(
            Carpeta carpeta,
            Long usuarioId,
            Long organizacionId) {

        PermisoEfectivo permisoEfectivo = evaluadorPermisos.evaluarPermisoCarpeta(
                usuarioId,
                carpeta.getId(),
                organizacionId);

        CapacidadesUsuario capacidades = convertirPermisoACapacidades(permisoEfectivo);

        return new CarpetaItem(
                carpeta.getId(),
                carpeta.getNombre(),
                carpeta.getDescripcion(),
                carpeta.getFechaCreacion(),
                carpeta.getFechaActualizacion(),
                0, // numSubcarpetas - no se detalla por ahora
                0,  // numDocumentos - no se detalla por ahora
                capacidades);
    }

    /**
     * Enriquece un documento con permisos específicos del usuario.
     */
    private DocumentoItem enriquecerDocumentoConPermisos(
            Documento documento,
            Long usuarioId,
            Long organizacionId) {

        PermisoEfectivo permisoEfectivo = evaluadorPermisos.evaluarPermisoDocumento(
                usuarioId,
                documento.getId(),
                organizacionId);

        CapacidadesUsuario capacidades = convertirPermisoACapacidades(permisoEfectivo);

        UsuarioResumen creadoPor = new UsuarioResumen(
                documento.getCreadoPor(),
                "Usuario " + documento.getCreadoPor()); // Placeholder - en realidad se debería obtener nombre del usuario

        return new DocumentoItem(
                documento.getId(),
                documento.getNombre(),
                documento.getExtension() != null ? documento.getExtension() : "", // Extension desde el modelo actualizado
                documento.getTamanioBytes() != null ? documento.getTamanioBytes() : 0L,  // Tamaño desde el modelo actualizado
                documento.getVersionActualId(), // Versión actual ID
                documento.getFechaCreacion(),
                documento.getFechaActualizacion(),
                creadoPor,
                capacidades);
    }


    // =================== Métodos Privados de Conversión ===================

    /**
     * Convierte un PermisoEfectivo a CapacidadesUsuario.
     */
    private CapacidadesUsuario convertirPermisoACapacidades(PermisoEfectivo permiso) {
        if (permiso == null) {
            // Sin permiso alguno
            return new CapacidadesUsuario(false, false, false, false);
        }

        NivelAcceso nivel = permiso.getNivelAcceso();
        boolean puedeLeer = nivel == NivelAcceso.LECTURA || 
                           nivel == NivelAcceso.ESCRITURA || 
                           nivel == NivelAcceso.ADMINISTRACION;
        boolean puedeEscribir = nivel == NivelAcceso.ESCRITURA || 
                               nivel == NivelAcceso.ADMINISTRACION;
        boolean puedeAdministrar = nivel == NivelAcceso.ADMINISTRACION;
        boolean puedeDescargar = puedeLeer; // Descargar requiere al menos lectura

        return new CapacidadesUsuario(puedeLeer, puedeEscribir, puedeAdministrar, puedeDescargar);
    }

    /**
     * Convierte OpcionesListado a Pageable para consultas de Spring Data.
     * 
     * <p>Nota: Spring Data PageRequest usa índice 0-based, pero OpcionesListado
     * mantiene páginas 1-based para la API. La conversión ocurre aquí.</p>
     */
    private Pageable convertirOpcionesAPageable(OpcionesListado opciones) {
        // Convertir de 1-based a 0-based
        int pageIndex = opciones.getPagina() - 1;
        
        return org.springframework.data.domain.PageRequest.of(
                pageIndex,
                opciones.getTamanio(),
                org.springframework.data.domain.Sort.by(opciones.getDireccion(), opciones.getCampoOrden()));
    }

    /**
     * Calcula el total de páginas dado un total de items y tamaño de página.
     */
    private int calcularTotalPaginas(int totalItems, int tamaniosPagina) {
        if (totalItems == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalItems / tamaniosPagina);
    }

    /**
     * Verifica si un nivel de acceso permite lectura.
     */
    private boolean tienePermisoLectura(NivelAcceso nivel) {
        return nivel == NivelAcceso.LECTURA || 
               nivel == NivelAcceso.ESCRITURA || 
               nivel == NivelAcceso.ADMINISTRACION;
    }
}
