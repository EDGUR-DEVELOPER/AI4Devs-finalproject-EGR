/**
 * Tipos relacionados con usuarios para el módulo de administración
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */

/** Estados posibles de un usuario */
export type UserStatus = 'ACTIVO' | 'INACTIVO';

/** Rol asignado a un usuario */
export interface UserRole {
    id: number;
    codigo: string;
    nombre: string;
}

/** Representa un usuario en el sistema de administración */
export interface AdminUser {
    id: string;
    email: string;
    nombreCompleto: string;
    estado: UserStatus;
    roles: UserRole[];
    fechaCreacion: string;
}

/** Datos para crear un nuevo usuario */
export interface CreateUserRequest {
    email: string;
    nombreCompleto: string;
    password: string;
    rolId?: number;
}

/** Datos del formulario de creación (sin idOrganizacion) */
export interface CreateUserFormData {
    email: string;
    nombre: string;
    password: string;
    rolId: number;
}

/** Metadata de paginación */
export interface PaginationMetadata {
    total: number;
    pagina: number;
    limite: number;
    totalPaginas: number;
}

/** Respuesta de la API al obtener usuarios */
export interface GetUsersResponse {
    usuarios: AdminUser[];
    paginacion: PaginationMetadata;
}

/** Respuesta de creación de usuario */
export interface CreateUserResponse {
    user: AdminUser;
}
