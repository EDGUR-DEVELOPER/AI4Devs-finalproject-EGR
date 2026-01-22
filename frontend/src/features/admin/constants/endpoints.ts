/**
 * Endpoints de la API de administración
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const ADMIN_ENDPOINTS = {
    USERS: {
        /** GET - Obtener lista de usuarios */
        LIST: '/iam/admin',
        /** POST - Crear nuevo usuario */
        CREATE: '/iam/admin',
        /** PATCH - Desactivar un usuario */
        DEACTIVATE: (userId: string) => `/iam/admin/${userId}/desactivar`,
        /** PATCH - Activar un usuario */
        ACTIVATE: (userId: string) => `/iam/admin/${userId}/activar`,
        /** POST - Asignar rol a usuario */
        ASSIGN_ROLE: (userId: string) => `/iam/admin/${userId}/roles`,
    },
    ROLES: {
        /** GET - Obtener roles disponibles */
        LIST: '/iam/roles',
    },
} as const;
