/**
 * Auth feature barrel export
 * Public API for the authentication feature
 */

// Main hook - primary interface for components
export { useAuth } from './hooks/useAuth';

// Types for external use
export type { 
  UserContext, 
  LoginResponse, 
  LogoutReason 
} from '@core/domain/auth/types';

// Utility function (US-AUTH-002 requirement)
export { getUserContextFromToken } from '@core/shared/utils/jwt';
