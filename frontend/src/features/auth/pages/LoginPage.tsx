import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { LoginForm } from '../components/LoginForm';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';

/**
 * Login page component
 * Displays the authentication form with centered layout
 * US-AUTH-006: Muestra alerta cuando la sesión ha expirado
 */
export const LoginPage = () => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showNotification } = useNotificationStore();

  // US-AUTH-006: Mostrar alerta de sesión expirada si viene desde logout forzado
  useEffect(() => {
    const reason = searchParams.get('reason');
    if (reason === 'expired') {
      showNotification(
        'Tu sesión ha expirado. Por favor, ingresa nuevamente.',
        'warning'
      );
      // Limpiar query params para evitar mostrar el mensaje múltiples veces
      searchParams.delete('reason');
      navigate({ search: searchParams.toString() }, { replace: true });
    }
  }, [searchParams, showNotification, navigate]);

  // Redirect to dashboard if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">
            Bienvenido
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            Sistema de Gestión Documental
          </p>
        </div>

        <div className="card bg-white p-8 shadow-md rounded-lg">
          <LoginForm />
        </div>

        <p className="text-center text-xs text-gray-500 mt-4">
          © {new Date().getFullYear()} Sistema de Gestión Documental
        </p>
      </div>
    </div>
  );
};
