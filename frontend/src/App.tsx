import { useEffect } from 'react';
import { BrowserRouter, useNavigate } from 'react-router-dom';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import { AppRouter } from '@core/shared/router';
import { ToastContainer } from '@ui/notifications/ToastContainer';

// Crear instancia de QueryClient
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

/**
 * App Component
 * Root component that wraps the entire application with necessary providers
 * Implements auth:logout event listener for centralized logout handling
 */
function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthLogoutListener />
        <AppRouter />
        <ToastContainer />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

/**
 * Listener component for auth:logout events
 * Centralizes logout redirection logic
 * US-AUTH-006: Agrega query param ?reason=expired para sesiones expiradas
 */
function AuthLogoutListener() {
  const navigate = useNavigate();

  useEffect(() => {
    const handleLogout = (event: Event) => {
      const customEvent = event as CustomEvent<{ reason: string }>;
      console.log('Auth logout event received:', customEvent.detail);
      
      // US-AUTH-006: Agregar query param para sesiones expiradas
      const reason = customEvent.detail?.reason;
      const shouldShowExpiredMessage = 
        reason === 'unauthorized' || reason === 'expired';
      
      // Redirect to login page con query param si es sesión expirada
      if (shouldShowExpiredMessage) {
        navigate('/login?reason=expired', { replace: true });
      } else {
        navigate('/login', { replace: true });
      }
    };

    window.addEventListener('auth:logout', handleLogout);

    return () => {
      window.removeEventListener('auth:logout', handleLogout);
    };
  }, [navigate]);

  return null;
}

export default App;
