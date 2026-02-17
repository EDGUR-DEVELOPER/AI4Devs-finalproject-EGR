package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * DTO for representing effective permission information in API responses.
 * 
 * <p>This DTO is returned by permission query endpoints to inform clients
 * about the user's effective permission on a resource (document or folder),
 * including the origin of that permission.</p>
 * 
 * <h3>Extended for US-ACL-006</h3>
 * <p>Now supports both document and folder permissions, with explicit origin tracking.</p>
 * 
 * @see com.docflow.documentcore.domain.model.PermisoEfectivo
 */
@Data
@Builder
public class PermisoEfectivoDTO {

    @JsonProperty("nivel_acceso")
    private String nivelAcceso;
    
    /**
     * Origin of permission: DOCUMENTO, CARPETA_DIRECTO, or CARPETA_HEREDADO
     */
    @JsonProperty("origen")
    private String origen;
    
    @JsonProperty("recurso_origen_id")
    private Long recursoOrigenId;
    
    @JsonProperty("tipo_recurso")
    private String tipoRecurso;
    
    @JsonProperty("evaluado_en")
    private OffsetDateTime evaluadoEn;

}

