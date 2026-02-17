/**
 * Feature: Dashboard
 * Página principal con información del sistema y acceso a módulos
 */

// Página
export { DashboardPage } from './pages/DashboardPage';

// Componentes reutilizables
export {
    DashboardLayout,
    DashboardHeader,
    DashboardIcon,
    WelcomeCard,
    ModuleCard,
    ModulesGrid,
    SystemInfoCard,
} from './components';

// Hooks
export { useDashboard } from './hooks/useDashboard';

// Tipos
export type {
    SystemModule,
    ModuleIcon,
    ModuleColor,
    SystemInfo,
    QuickStat,
} from './types/dashboard.types';

// Constantes
export { SYSTEM_MODULES, SYSTEM_INFO, DASHBOARD_MESSAGES } from './constants/modules';
