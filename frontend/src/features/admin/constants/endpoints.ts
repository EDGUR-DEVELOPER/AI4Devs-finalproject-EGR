/**
 * Endpoints de la API de administración
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const ADMIN_ENDPOINTS = {
    USERS: {
        /** GET - Obtener lista de usuarios */
        LIST: '/iam/admin/users',
        /** POST - Crear nuevo usuario */
        CREATE: '/iam/admin/users',
        /** PATCH - Desactivar un usuario */
        DEACTIVATE: (userId: string) => `/iam/admin/users/${userId}/deactivate`,
        /** POST - Asignar rol a usuario */
        ASSIGN_ROLE: (userId: string) => `/iam/admin/users/${userId}/roles`,
    },
    ROLES: {
        /** GET - Obtener roles disponibles */
        LIST: '/iam/admin/roles',
    },
} as const;
