import type { SystemModule, QuickAction, SystemInfo } from '../types/dashboard.types';

/**
 * Configuración de módulos del sistema
 * Define todos los módulos disponibles en el dashboard
 */
export const SYSTEM_MODULES: SystemModule[] = [
    {
        id: 'users',
        name: 'Gestión de Usuarios',
        description: 'Administra usuarios, roles y permisos',
        icon: 'users',
        path: '/admin',
        color: 'blue',
        requiredRoles: ['ADMIN', 'SUPER_ADMIN'],
        enabled: true,
    },
    {
        id: 'documents',
        name: 'Documentos',
        description: 'Gestiona el flujo de documentos',
        icon: 'documents',
        path: '/documents',
        color: 'green',
        enabled: true,
    },
    {
        id: 'workflow',
        name: 'Flujo de Trabajo',
        description: 'Configura flujos y automatizaciones',
        icon: 'workflow',
        path: '/workflow',
        color: 'purple',
        enabled: false, // Próximamente
    },
    {
        id: 'reports',
        name: 'Reportes',
        description: 'Visualiza estadísticas y reportes',
        icon: 'reports',
        path: '/reports',
        color: 'orange',
        enabled: false, // Próximamente
    },
    {
        id: 'audit',
        name: 'Auditoría',
        description: 'Revisa el historial de actividades',
        icon: 'audit',
        path: '/audit',
        color: 'teal',
        requiredRoles: ['ADMIN', 'AUDITOR'],
        enabled: true,
    },
    {
        id: 'settings',
        name: 'Configuración',
        description: 'Ajustes del sistema y preferencias',
        icon: 'settings',
        path: '/settings',
        color: 'indigo',
        requiredRoles: ['ADMIN'],
        enabled: false, // Próximamente
    },
];

/**
 * Acciones rápidas del dashboard
 */
export const QUICK_ACTIONS: QuickAction[] = [
    {
        id: 'new-document',
        label: 'Nuevo Documento',
        icon: 'documents',
        path: '/documents/new',
        variant: 'primary',
    },
    {
        id: 'view-pending',
        label: 'Ver Pendientes',
        icon: 'workflow',
        path: '/documents?status=pending',
        variant: 'secondary',
    },
];

/**
 * Información del sistema
 */
export const SYSTEM_INFO: SystemInfo = {
    appName: 'DocFlow',
    version: '1.0.0',
    environment: 'development',
};

/**
 * Mensajes del dashboard
 */
export const DASHBOARD_MESSAGES = {
    WELCOME: (name: string) => `¡Hola, ${name}!`,
    WELCOME_BACK: 'Bienvenido de vuelta',
    QUICK_ACCESS: 'Acceso Rápido',
    MODULES: 'Módulos',
    SYSTEM_INFO: 'Información del Sistema',
    COMING_SOON: 'Próximamente',
    NO_ACCESS: 'Sin acceso',
    VERSION: 'Versión',
    ENVIRONMENT: 'Entorno',
    LAST_LOGIN: 'Último acceso',
    NO_MODULES_AVAILABLE: 'No hay módulos disponibles',
} as const;
