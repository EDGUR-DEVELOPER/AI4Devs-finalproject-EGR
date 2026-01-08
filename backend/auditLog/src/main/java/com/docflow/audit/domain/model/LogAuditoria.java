package com.docflow.audit.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entidad que representa un evento de auditoría en el sistema.
 * 
 * Almacenada en MongoDB para alta performance en escritura y consultas temporales.
 * Los eventos son inmutables - no se modifican ni eliminan (salvo por TTL).
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Índice compuesto (organizacion_id, fecha_evento) para queries optimizadas
 * - MongoEventListener inyecta organizacionId automáticamente en inserts
 * - TTL index elimina automáticamente logs después de 730 días (2 años)
 * 
 * RETENCIÓN:
 * - Los logs se retienen por 2 años para compliance
 * - Después de 730 días, MongoDB elimina automáticamente el documento (TTL)
 */
@Document(collection = "logs_auditoria")
@CompoundIndex(
    name = "idx_tenant_fecha", 
    def = "{'organizacionId': 1, 'fechaEvento': -1}"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogAuditoria {
    
    /**
     * ID único del log (generado por MongoDB).
     */
    @Id
    private String id;
    
    /**
     * ID de la organización (tenant) - discriminador multi-tenant crítico.
     */
    @Indexed
    private Integer organizacionId;
    
    /**
     * ID del usuario que generó el evento (puede ser null si fue proceso automatizado).
     */
    @Indexed
    private Long usuarioId;
    
    /**
     * Código del evento (ej: DOC_CREATED, DOC_DELETED, ACL_CHANGED, USER_LOGIN).
     */
    @Indexed
    private String codigoEvento;
    
    /**
     * Detalles del cambio en formato JSONB.
     * 
     * Ejemplo: {"antes": {"nombre": "A"}, "despues": {"nombre": "B"}}
     * Ejemplo: {"campo": "estado", "valor_anterior": "borrador", "valor_nuevo": "publicado"}
     */
    private Map<String, Object> detallesCambio;
    
    /**
     * Dirección IP del cliente que realizó la acción (IPv4 o IPv6).
     */
    private String direccionIp;
    
    /**
     * Fecha y hora del evento.
     * 
     * IMPORTANTE: Este campo tiene TTL index de 730 días.
     * MongoDB eliminará automáticamente el documento 2 años después de esta fecha.
     */
    @Indexed(expireAfterSeconds = 63072000) // 730 días * 24h * 60min * 60seg = 63,072,000 segundos
    private OffsetDateTime fechaEvento;
    
    /**
     * Metadatos adicionales opcionales (user agent, request ID, trace ID, etc.).
     */
    private Map<String, Object> metadatos;
}
