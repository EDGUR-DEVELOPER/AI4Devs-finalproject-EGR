/**
 * Mensajes para el módulo de administración
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */

/** Mensajes de éxito para operaciones */
export const SUCCESS_MESSAGES = {
    USER_CREATED: 'Usuario creado exitosamente',
    USER_DEACTIVATED: 'Usuario desactivado exitosamente',
    ROLE_ASSIGNED: 'Rol asignado exitosamente',
} as const;

/** Mensajes de error mapeados desde códigos HTTP */
export const ERROR_MESSAGES: Record<number, string> = {
    400: 'Datos inválidos. Verifica la información ingresada.',
    403: 'No tienes permisos para realizar esta acción.',
    409: 'El email ya está registrado en el sistema.',
    500: 'Error interno del servidor. Intenta más tarde.',
};

/** Mensaje de error por defecto */
export const DEFAULT_ERROR_MESSAGE = 'Error desconocido. Intenta nuevamente.';

/** Mensajes de interfaz de usuario */
export const UI_MESSAGES = {
    EMPTY_TABLE: 'No hay usuarios registrados',
    LOADING: 'Cargando usuarios...',
    LOAD_ERROR: 'Error al cargar usuarios. Intenta nuevamente.',
    CONFIRM_DEACTIVATE: (userName: string) =>
        `¿Estás seguro de desactivar al usuario ${userName}?`,
    RETRY_BUTTON: 'Reintentar',
    NO_ROLES: 'Sin roles',
    ADD_ROLE: 'Agregar rol',
} as const;

/** Etiquetas de formulario */
export const FORM_LABELS = {
    EMAIL: 'Email',
    NAME: 'Nombre',
    PASSWORD: 'Contraseña',
    CREATE_USER: 'Crear Usuario',
    CANCEL: 'Cancelar',
    DEACTIVATE: 'Desactivar',
    CONFIRM: 'Confirmar',
} as const;

/** Mensajes de validación de formulario */
export const VALIDATION_MESSAGES = {
    EMAIL_REQUIRED: 'El email es requerido',
    EMAIL_INVALID: 'El formato del email no es válido',
    NAME_REQUIRED: 'El nombre es requerido',
    PASSWORD_REQUIRED: 'La contraseña es requerida',
    PASSWORD_MIN_LENGTH: 'La contraseña debe tener al menos 8 caracteres',
} as const;
