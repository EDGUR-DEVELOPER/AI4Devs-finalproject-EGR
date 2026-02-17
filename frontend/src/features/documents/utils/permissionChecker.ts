/**
 * Permission Checker Utilities
 * US-DOC-006: Verificación de permisos del usuario basado en JWT
 */

import { jwtDecode } from 'jwt-decode';

/**
 * Estructura del payload JWT con permisos
 */
interface JwtPayload {
  sub: string; // userId
  email: string;
  roles: string[];
  permissions?: string[]; // Formato: {resourceType}:{action}:{resourceId}
  exp: number;
  iat: number;
}

/**
 * Información extraída del usuario desde JWT
 */
export interface UserInfo {
  id: string;
  email: string;
  roles: string[];
  permissions: string[];
}

/**
 * Decodifica un JWT y retorna su payload
 * @param token - JWT token a decodificar
 * @returns Payload decodificado o null si es inválido
 */
export function parseJwt(token: string): JwtPayload | null {
  try {
    return jwtDecode<JwtPayload>(token);
  } catch (error) {
    console.error('Error decodificando JWT:', error);
    return null;
  }
}

/**
 * Extrae información del usuario desde el JWT
 */
export function extractUserInfo(token: string): UserInfo | null {
  const payload = parseJwt(token);
  if (!payload) return null;

  return {
    id: payload.sub,
    email: payload.email,
    roles: payload.roles || [],
    permissions: payload.permissions || [],
  };
}

/**
 * Verifica si el usuario tiene un permiso específico
 * @param token - JWT token del usuario
 * @param resourceType - Tipo de recurso (ej: 'documents', 'folders')
 * @param action - Acción requerida (ej: 'read', 'write')
 * @param resourceId - ID del recurso específico
 * @returns true si el usuario tiene el permiso
 */
export function hasPermission(
  token: string,
  resourceType: string,
  action: string,
  resourceId: string
): boolean {
  const userInfo = extractUserInfo(token);
  if (!userInfo) return false;

  // Formato esperado: {resourceType}:{action}:{resourceId}
  const requiredPermission = `${resourceType}:${action}:${resourceId}`;
  
  // Verificar si el permiso exacto existe
  if (userInfo.permissions.includes(requiredPermission)) {
    return true;
  }

  // Verificar permisos con comodín (ej: documents:write:*)
  const wildcardPermission = `${resourceType}:${action}:*`;
  if (userInfo.permissions.includes(wildcardPermission)) {
    return true;
  }

  return false;
}

/**
 * Verifica si el usuario puede escribir (subir documentos) en una carpeta
 */
export function canUserWriteToFolder(token: string, folderId: string): boolean {
  return hasPermission(token, 'documents', 'write', folderId);
}

/**
 * Verifica si el usuario puede leer documentos de una carpeta
 */
export function canUserReadFolder(token: string, folderId: string): boolean {
  return hasPermission(token, 'documents', 'read', folderId);
}
