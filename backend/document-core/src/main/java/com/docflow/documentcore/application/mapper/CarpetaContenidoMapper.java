package com.docflow.documentcore.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.docflow.documentcore.application.dto.CapacidadesDTO;
import com.docflow.documentcore.application.dto.CarpetaItemDTO;
import com.docflow.documentcore.application.dto.ContenidoCarpetaDTO;
import com.docflow.documentcore.application.dto.DocumentoItemDTO;
import com.docflow.documentcore.application.dto.UsuarioResumenDTO;
import com.docflow.documentcore.domain.model.CapacidadesUsuario;
import com.docflow.documentcore.domain.model.CarpetaItem;
import com.docflow.documentcore.domain.model.ContenidoCarpeta;
import com.docflow.documentcore.domain.model.DocumentoItem;
import com.docflow.documentcore.domain.model.UsuarioResumen;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Mapper de MapStruct para convertir modelos de dominio a DTOs.
 * 
 * <p>Responsable de transformar:
 * <ul>
 *   <li>CapacidadesUsuario → CapacidadesDTO</li>
 *   <li>UsuarioResumen → UsuarioResumenDTO</li>
 *   <li>CarpetaItem → CarpetaItemDTO</li>
 *   <li>DocumentoItem → DocumentoItemDTO</li>
 *   <li>ContenidoCarpeta → ContenidoCarpetaDTO</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Mapper(componentModel = "spring")
public interface CarpetaContenidoMapper {

    /**
     * Convierte CapacidadesUsuario a CapacidadesDTO.
     */
    CapacidadesDTO toCapacidadesDTO(CapacidadesUsuario capacidades);

    /**
     * Convierte UsuarioResumen a UsuarioResumenDTO.
     */
    UsuarioResumenDTO toUsuarioResumenDTO(UsuarioResumen usuario);

    /**
     * Convierte CarpetaItem a CarpetaItemDTO.
     */
    @Mapping(source = "fechaActualizacion", target = "fechaModificacion")
    CarpetaItemDTO toCarpetaItemDTO(CarpetaItem carpeta);

    /**
     * Convierte DocumentoItem a DocumentoItemDTO.
     */
    @Mapping(source = "versionActualId", target = "versionActual")
    @Mapping(source = "fechaActualizacion", target = "fechaModificacion")
    DocumentoItemDTO toDocumentoItemDTO(DocumentoItem documento);

    /**
     * Convierte ContenidoCarpeta a ContenidoCarpetaDTO.
     */
    ContenidoCarpetaDTO toContenidoCarpetaDTO(ContenidoCarpeta contenido);

    // Helper methods para conversión de tipos de fecha
    
    /**
     * Convierte Instant a LocalDateTime usando la zona horaria del sistema.
     */
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Convierte OffsetDateTime a LocalDateTime.
     */
    default LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.toLocalDateTime();
    }
}
