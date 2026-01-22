import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload, UserContext, LogoutReason } from '@core/domain/auth/types';
import { isTokenExpired } from '@core/shared/utils/jwt';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { LOGOUT_MESSAGES } from '../constants/messages';

interface AuthState extends UserContext {
  /** JWT token string */
  token: string | null;
  /** Flag to prevent race conditions during logout */
  isLoggingOut: boolean;
  /** Set the authentication token and decode user context */
  setToken: (token: string) => void;
  /** Clear authentication state and notify user */
  logout: (reason: LogoutReason) => void;
  /** Check if the current token is expired */
  checkTokenExpiration: () => void;
}

/**
 * Authentication store with Zustand
 * Manages user authentication state with persistence
 * Implements US-AUTH-002 requirements
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      // Initial state
      userId: '',
      organizacionId: 0,
      roles: [],
      isAuthenticated: false,
      token: null,
      isLoggingOut: false,

      setToken: (token: string) => {
        try {
          const decoded = jwtDecode<JwtPayload>(token);

          // Check if token is already expired
          if (isTokenExpired(decoded)) {
            console.warn('Attempted to set an expired token');
            get().logout('expired');
            return;
          }

          // Update state with decoded information
          set({
            token,
            userId: decoded.sub,
            organizacionId: decoded.organizacion_id,
            roles: decoded.roles,
            isAuthenticated: true,
            isLoggingOut: false,
          });

          // Store token in localStorage for Axios interceptor
          localStorage.setItem('authToken', token);
        } catch (error) {
          console.error('Failed to decode token:', error);
          get().logout('unauthorized');
        }
      },

      logout: (reason: LogoutReason) => {
        const state = get();
        
        // Prevent multiple simultaneous logout calls
        if (state.isLoggingOut) {
          return;
        }

        set({ isLoggingOut: true });

        // Clear state
        set({
          token: null,
          userId: '',
          organizacionId: 0,
          roles: [],
          isAuthenticated: false,
        });

        // Clear localStorage
        localStorage.removeItem('authToken');
        localStorage.removeItem('auth-storage');

        // Show notification to user
        const message = LOGOUT_MESSAGES[reason];
        const notificationType = reason === 'manual' ? 'info' : 'warning';
        useNotificationStore.getState().showNotification(message, notificationType);

        // Emit custom event for App.tsx to handle navigation
        window.dispatchEvent(
          new CustomEvent('auth:logout', { detail: { reason } })
        );

        // Reset flag after a short delay
        setTimeout(() => {
          set({ isLoggingOut: false });
        }, 100);
      },

      checkTokenExpiration: () => {
        const { token, isLoggingOut } = get();
        
        if (!token || isLoggingOut) {
          return;
        }

        try {
          const decoded = jwtDecode<JwtPayload>(token);
          
          if (isTokenExpired(decoded)) {
            get().logout('expired');
          }
        } catch (error) {
          console.error('Failed to check token expiration:', error);
          get().logout('unauthorized');
        }
      },
    }),
    {
      name: 'auth-storage',
      // Rehydrate and check expiration on load
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.checkTokenExpiration();
        }
      },
    }
  )
);

// Listen for storage changes in other tabs (cross-tab synchronization)
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key === 'auth-storage') {
      const state = useAuthStore.getState();
      
      // Avoid race conditions
      if (state.isLoggingOut) {
        return;
      }

      // Check if auth was cleared in another tab
      if (!event.newValue) {
        state.logout('expired');
        return;
      }

      // Parse new value and check if token expired
      try {
        const newState = JSON.parse(event.newValue);
        if (newState.state?.token) {
          const decoded = jwtDecode<JwtPayload>(newState.state.token);
          if (isTokenExpired(decoded)) {
            state.logout('expired');
          }
        } else {
          state.logout('expired');
        }
      } catch (error) {
        console.error('Failed to parse storage event:', error);
      }
    }
  });
}
