import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@features/auth/hooks/useAuthStore';

interface AdminRouteGuardProps {
    /** Contenido a renderizar si tiene acceso */
    children: React.ReactNode;
}

/** Códigos de roles permitidos para administración */
const ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN'];

/**
 * Guard que protege rutas de administración
 * Verifica si el usuario tiene rol ADMIN en su token
 * Redirige a login si no está autenticado
 * Redirige a access-denied si no tiene permisos
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const AdminRouteGuard: React.FC<AdminRouteGuardProps> = ({ children }) => {
    const { roles, isAuthenticated } = useAuthStore();

    // Redirigir a login si no está autenticado
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    // Verificar si tiene algún rol permitido
    const hasAllowedRole = roles.some((role) => ADMIN_ROLES.includes(role));

    if (!hasAllowedRole) {
        return <Navigate to="/access-denied" replace />;
    }

    return <>{children}</>;
};
