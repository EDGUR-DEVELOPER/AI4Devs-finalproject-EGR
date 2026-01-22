/**
 * Tipos relacionados con roles para el módulo de administración
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */

/** Rol disponible para asignar a usuarios */
export interface AvailableRole {
    id: number;
    codigo: string;
    nombre: string;
}

/** Respuesta de la API al obtener roles disponibles */
export type GetRolesResponse = AvailableRole[];

/** Request para asignar un rol a un usuario */
export interface AssignRoleRequest {
    roleId: number;
}
