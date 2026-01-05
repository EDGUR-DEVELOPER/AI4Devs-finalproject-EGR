import { useAuthStore } from './useAuthStore';
import { authApi } from '../api/authApi';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';

/**
 * Custom hook for authentication
 * Public interface for auth feature - implements US-AUTH-002 requirements
 * 
 * Exposes:
 * - isAuthenticated: boolean
 * - userId: string
 * - organizacionId: number
 * - roles: string[]
 * - login(email, password): Promise
 * - logout(): void
 */
export function useAuth() {
  const { 
    token, 
    userId, 
    organizacionId, 
    roles, 
    isAuthenticated,
    setToken, 
    logout: logoutStore 
  } = useAuthStore();

  const showNotification = useNotificationStore(
    (state) => state.showNotification
  );

  /**
   * Login user with email and password
   * @param email - User email
   * @param password - User password
   */
  const login = async (email: string, password: string) => {
    try {
      const response = await authApi.login(email, password);
      setToken(response.token);
      showNotification('Sesión iniciada correctamente', 'success');
      return response;
    } catch (error) {
      console.error('Login failed:', error);
      showNotification('Error al iniciar sesión. Verifica tus credenciales', 'error');
      throw error;
    }
  };

  /**
   * Logout user manually
   */
  const logout = () => {
    logoutStore('manual');
  };

  /**
   * Switch organization context
   * @param newOrganizacionId - New organization ID
   */
  const switchOrganization = async (newOrganizacionId: number) => {
    try {
      const response = await authApi.switchOrganization(newOrganizacionId);
      setToken(response.token);
      showNotification('Organización cambiada correctamente', 'success');
      return response;
    } catch (error) {
      console.error('Switch organization failed:', error);
      showNotification('Error al cambiar de organización', 'error');
      throw error;
    }
  };

  return {
    // User context from token (US-AUTH-002 requirement)
    isAuthenticated,
    userId,
    organizacionId,
    roles,
    token,
    
    // Actions
    login,
    logout,
    switchOrganization,
  };
}
