package com.docflow.documentcore.application.validator;

import com.docflow.documentcore.domain.exception.carpeta.CarpetaNombreDuplicadoException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.SinPermisoCarpetaException;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import org.springframework.stereotype.Component;

/**
 * Validador de reglas de negocio para operaciones con carpetas.
 * 
 * <p>Centraliza las validaciones complejas que involucran consultas a la base de datos
 * y lógica de permisos. Las validaciones simples (formato, tamaño) se hacen en el modelo
 * de dominio mismo.</p>
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>Validar existencia de carpeta padre</li>
 *   <li>Validar unicidad de nombres por nivel</li>
 *   <li>Validar permisos del usuario (integración con ACL)</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Component
public class CarpetaValidator {
    
    private final ICarpetaRepository carpetaRepository;
    // TODO: Inyectar PermissionEvaluator cuando esté disponible (US-ACL-006)
    
    public CarpetaValidator(ICarpetaRepository carpetaRepository) {
        this.carpetaRepository = carpetaRepository;
    }
    
    /**
     * Valida que la carpeta padre existe y pertenece a la misma organización.
     * 
     * @param carpetaPadreId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @throws CarpetaNotFoundException si la carpeta padre no existe
     */
    public void validarCarpetaPadreExiste(Long carpetaPadreId, Long organizacionId) {
        if (carpetaPadreId == null) {
            return;  // Es válido crear carpetas raíz (aunque normalmente ya existe una)
        }
        
        boolean existe = carpetaRepository.obtenerPorId(carpetaPadreId, organizacionId).isPresent();
        
        if (!existe) {
            throw new CarpetaNotFoundException(carpetaPadreId);
        }
    }
    
    /**
     * Valida que no existe otra carpeta con el mismo nombre en el nivel.
     * 
     * @param nombre nombre de la carpeta a validar
     * @param carpetaPadreId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @throws CarpetaNombreDuplicadoException si ya existe una carpeta con ese nombre
     */
    public void validarNombreUnico(String nombre, Long carpetaPadreId, Long organizacionId) {
        boolean existe = carpetaRepository.nombreExisteEnNivel(nombre, carpetaPadreId, organizacionId);
        
        if (existe) {
            throw new CarpetaNombreDuplicadoException(nombre, carpetaPadreId);
        }
    }
    
    /**
     * Valida que el usuario tiene permisos para crear carpetas en la carpeta padre.
     * 
     * <p><strong>Requisito:</strong> Permiso de ESCRITURA o ADMINISTRACION en carpeta padre.</p>
     * 
     * <p><strong>TODO:</strong> Integrar con PermissionEvaluator real cuando US-ACL-006 esté lista.
     * Por ahora usa un stub que siempre permite.</p>
     * 
     * @param usuarioId identificador del usuario
     * @param carpetaPadreId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @throws SinPermisoCarpetaException si el usuario no tiene permisos suficientes
     */
    public void validarPermisos(Long usuarioId, Long carpetaPadreId, Long organizacionId) {
        // TODO: Implementar integración real con sistema ACL
        // Por ahora, STUB que siempre permite (para desarrollo inicial)
        
        // Ejemplo de integración futura:
        // boolean tienePermiso = permissionEvaluator.hasPermission(
        //     usuarioId,
        //     carpetaPadreId,
        //     "CARPETA",
        //     NivelAcceso.ESCRITURA
        // );
        //
        // if (!tienePermiso) {
        //     throw new SinPermisoCarpetaException(carpetaPadreId);
        // }
        
        // STUB temporal: siempre permitir
        // Comentar esta línea cuando se integre con ACL real
    }
}
