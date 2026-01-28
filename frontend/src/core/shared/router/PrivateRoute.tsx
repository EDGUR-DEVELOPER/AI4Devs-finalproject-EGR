import { Navigate } from 'react-router-dom';
import { useAuth } from '../../../features/auth/hooks/useAuth';

export interface PrivateRouteProps {
  children: React.ReactNode;
}

/**
 * Private route wrapper component
 * Redirects to login if user is not authenticated
 */
export const PrivateRoute: React.FC<PrivateRouteProps> = ({ children }) => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};
