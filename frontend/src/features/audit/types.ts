/**
 * Tipos y enums para el módulo de auditoría
 * Preparado para integración Post-MVP con backend de auditoría
 */

/**
 * Códigos de eventos auditables en el sistema
 */
export const AuditEventCode = {
  // Seguridad
  ACCESS_DENIED: 'ACCESS_DENIED',
  ACCESS_DENIED_404: 'ACCESS_DENIED_404',
  
  // Autenticación
  USER_LOGIN: 'USER_LOGIN',
  USER_LOGOUT: 'USER_LOGOUT',
  USER_SWITCH_ORG: 'USER_SWITCH_ORG',
  
  // Documentos
  DOC_UPLOAD: 'DOC_UPLOAD',
  DOC_DOWNLOAD: 'DOC_DOWNLOAD',
  DOC_CREATED: 'DOC_CREATED',
  DOC_DELETED: 'DOC_DELETED',
  DOC_VERSION_CREATE: 'DOC_VERSION_CREATE',
  
  // Carpetas
  FOLDER_CREATE: 'FOLDER_CREATE',
  FOLDER_DELETE: 'FOLDER_DELETE',
  
  // Administración
  USER_CREATE: 'USER_CREATE',
  USER_DEACTIVATE: 'USER_DEACTIVATE',
  
  // Permisos
  PERMISSION_GRANT: 'PERMISSION_GRANT',
  PERMISSION_REVOKE: 'PERMISSION_REVOKE',
  ACL_CHANGED: 'ACL_CHANGED',
} as const;

export type AuditEventCode = typeof AuditEventCode[keyof typeof AuditEventCode];

/**
 * Detalles específicos del evento de auditoría
 * GDPR-friendly: Solo incluye información necesaria para seguridad
 */
export interface AuditEventDetails {
  recurso: string;        // URL del recurso al que se intentó acceder
  metodo: string;         // Método HTTP (GET, POST, etc.)
  statusCode?: number;    // Código de respuesta HTTP
  mensaje?: string;       // Mensaje descriptivo opcional
}

/**
 * Metadata adicional del contexto de la petición
 */
export interface AuditEventMetadata {
  userAgent?: string;     // User agent del navegador
  timestamp: string;      // ISO 8601 timestamp
}

/**
 * Request completo para registrar un evento de auditoría
 * El organizacionId se extrae automáticamente del token JWT en backend
 */
export interface AuditEventRequest {
  codigoEvento: AuditEventCode;           // Código del evento
  detallesCambio: AuditEventDetails;      // Detalles del evento
  metadatos?: AuditEventMetadata;         // Metadata adicional opcional
}

/**
 * Respuesta del servidor al registrar un evento
 */
export interface AuditEventResponse {
  id: string;                             // ID del evento registrado
  fechaEvento: string;                    // Timestamp del evento
}
