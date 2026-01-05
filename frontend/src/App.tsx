import { useEffect } from 'react';
import { BrowserRouter, useNavigate } from 'react-router-dom';
import { AppRouter } from '@core/shared/router';
import { ToastContainer } from '@ui/notifications/ToastContainer';

/**
 * App Component
 * Root component that wraps the entire application with necessary providers
 * Implements auth:logout event listener for centralized logout handling
 */
function App() {
  return (
    <BrowserRouter>
      <AuthLogoutListener />
      <AppRouter />
      <ToastContainer />
    </BrowserRouter>
  );
}

/**
 * Listener component for auth:logout events
 * Centralizes logout redirection logic
 */
function AuthLogoutListener() {
  const navigate = useNavigate();

  useEffect(() => {
    const handleLogout = (event: Event) => {
      const customEvent = event as CustomEvent<{ reason: string }>;
      console.log('Auth logout event received:', customEvent.detail);
      
      // Redirect to login page
      navigate('/login', { replace: true });
    };

    window.addEventListener('auth:logout', handleLogout);

    return () => {
      window.removeEventListener('auth:logout', handleLogout);
    };
  }, [navigate]);

  return null;
}

export default App;
