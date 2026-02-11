import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { LoginPage } from '../../../features/auth/pages/LoginPage';
import { PrivateRoute } from './PrivateRoute';
import { NotFoundPage } from '../../../common/ui/pages/NotFoundPage';
import { DashboardPage } from '../../../features/dashboard/pages/DashboardPage';
import { UsersManagementPage, AccessDeniedPage, AdminRouteGuard } from '../../../features/admin';
import { FoldersPage } from '../../../features/folders';

/**
 * Application router
 * Defines all routes for the application
 * Note: BrowserRouter is now in App.tsx to allow useNavigate in AuthLogoutListener
 */
export function AppRouter() {
  const navigate = useNavigate();

  // Listener para evento custom 'resource-not-found' emitido por el interceptor de Axios
  // Redirige a la página 404 cuando se detecta un intento de acceso a recurso de otra organización
  useEffect(() => {
    const handleResourceNotFound = (event: Event) => {
      const customEvent = event as CustomEvent;
      console.debug(
        '[Router] Evento resource-not-found detectado:',
        customEvent.detail
      );
      
      // Navegar a página 404 reemplazando la entrada actual en el historial
      // (evita que el usuario vuelva atrás al recurso inaccesible)
      navigate('/404', { replace: true });
    };

    // Agregar listener al montar el componente
    window.addEventListener('resource-not-found', handleResourceNotFound);

    // Cleanup: Remover listener al desmontar
    return () => {
      window.removeEventListener('resource-not-found', handleResourceNotFound);
    };
  }, [navigate]);

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route 
        path="/dashboard" 
        element={
          <PrivateRoute>
            <DashboardPage />
          </PrivateRoute>
        } 
      />
      
      {/* Rutas protegidas para navegación de carpetas */}
      <Route
        path="/carpetas"
        element={
          <PrivateRoute>
            <FoldersPage />
          </PrivateRoute>
        }
      />
      <Route
        path="/carpetas/:id"
        element={
          <PrivateRoute>
            <FoldersPage />
          </PrivateRoute>
        }
      />
      
      {/* Ruta protegida para administración de usuarios */}
      <Route
        path="/admin/*"
        element={
          <AdminRouteGuard>
            <UsersManagementPage />
          </AdminRouteGuard>
        }
      />

      {/* Ruta de acceso denegado */}
      <Route path="/access-denied" element={<AccessDeniedPage />} />

      {/* Ruta dedicada para errores 404 de recursos */}
      <Route path="/404" element={<NotFoundPage />} />

      {/* Catch-all: Cualquier ruta no definida */}
      <Route path="*" element={<NotFoundPage message="Página no encontrada" />} />
    </Routes>
  );
}
