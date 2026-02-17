package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.PermisoEfectivoDTO;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting PermisoEfectivo domain model to PermisoEfectivoDTO.
 * 
 * <p>This mapper handles the conversion from the rich domain model to a simpler
 * DTO suitable for API responses.</p>
 */
@Component
public class PermisoEfectivoMapper {
    
    /**
     * Converts a PermisoEfectivo domain model to PermisoEfectivoDTO.
     * 
     * @param permiso the domain model (can be null)
     * @return the DTO or null if input is null
     */
    public PermisoEfectivoDTO toDto(PermisoEfectivo permiso) {
        if (permiso == null) {
            return null;
        }
        
        return PermisoEfectivoDTO.builder()
                .nivelAcceso(permiso.getNivelAcceso().name())
                .origen(permiso.getOrigen().name())
                .recursoOrigenId(permiso.getRecursoOrigenId())
                .tipoRecurso(permiso.getTipoRecurso().name())
                .evaluadoEn(permiso.getEvaluadoEn())
                .build();
    }
}
