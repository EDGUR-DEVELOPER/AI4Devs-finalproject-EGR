import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { LoginForm } from '../components/LoginForm';

/**
 * Login page component
 * Displays the authentication form with centered layout
 */
export const LoginPage = () => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

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
