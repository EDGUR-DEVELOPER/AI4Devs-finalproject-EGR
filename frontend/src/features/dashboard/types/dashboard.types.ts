/**
 * Tipos para el módulo de Dashboard
 * Feature: Dashboard con información del sistema y acceso a módulos
 */

/** Información de un módulo del sistema */
export interface SystemModule {
    id: string;
    name: string;
    description: string;
    icon: ModuleIcon;
    path: string;
    color: ModuleColor;
    /** Roles requeridos para ver este módulo */
    requiredRoles?: string[];
    /** Indica si el módulo está habilitado */
    enabled: boolean;
}

/** Iconos disponibles para módulos */
export type ModuleIcon =
    | 'users'
    | 'documents'
    | 'settings'
    | 'reports'
    | 'audit'
    | 'organization'
    | 'workflow'
    | 'calendar';

/** Colores disponibles para módulos */
export type ModuleColor =
    | 'blue'
    | 'green'
    | 'purple'
    | 'orange'
    | 'red'
    | 'teal'
    | 'indigo'
    | 'pink';

/** Información del sistema para mostrar en dashboard */
export interface SystemInfo {
    appName: string;
    version: string;
    environment: 'development' | 'staging' | 'production';
    lastLogin?: string;
}

/** Estadística rápida para el dashboard */
export interface QuickStat {
    label: string;
    value: string | number;
    icon: ModuleIcon;
    trend?: 'up' | 'down' | 'neutral';
    trendValue?: string;
}

/** Acción rápida del dashboard */
export interface QuickAction {
    id: string;
    label: string;
    icon: ModuleIcon;
    path: string;
    variant: 'primary' | 'secondary';
}
