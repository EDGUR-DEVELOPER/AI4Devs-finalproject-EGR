/**
 * Feature: Admin User Management
 * US-ADMIN-005: UI mínima de gestión de usuarios
 * 
 * Este módulo exporta todos los componentes, hooks y utilidades
 * necesarios para la administración de usuarios
 */

// Páginas
export { UsersManagementPage } from './pages/UsersManagementPage';
export { AccessDeniedPage } from './pages/AccessDeniedPage';

// Guards
export { AdminRouteGuard } from './guards/AdminRouteGuard';

// Hooks
export { useAdminUsers } from './hooks/useAdminUsers';
export { useAdminRoles } from './hooks/useAdminRoles';
export { useCreateUser } from './hooks/useCreateUser';
export { useDeactivateUser } from './hooks/useDeactivateUser';
export { useAssignRole } from './hooks/useAssignRole';

// Componentes (para uso externo si es necesario)
export { UsersTable } from './components/UsersTable';
export { CreateUserModal } from './components/CreateUserModal';
export { ConfirmationModal } from './components/ConfirmationModal';
export { StatusBadge } from './components/StatusBadge';
export { RoleBadge, RoleDropdown } from './components/RoleAssignment';

// Tipos
export type { AdminUser, UserStatus, UserRole, CreateUserRequest } from './types/user.types';
export type { AvailableRole } from './types/role.types';
