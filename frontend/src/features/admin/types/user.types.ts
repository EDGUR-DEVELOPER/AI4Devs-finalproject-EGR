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
    nombre: string;
    estado: UserStatus;
    roles: UserRole[];
    createdAt: string;
}

/** Datos para crear un nuevo usuario */
export interface CreateUserRequest {
    email: string;
    nombre: string;
    password: string;
}

/** Respuesta de la API al obtener usuarios */
export interface GetUsersResponse {
    users: AdminUser[];
}

/** Respuesta de creación de usuario */
export interface CreateUserResponse {
    user: AdminUser;
}
