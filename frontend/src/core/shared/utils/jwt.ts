import { jwtDecode } from 'jwt-decode';
import type { JwtPayload, UserContext } from '@core/domain/auth/types';

/**
 * Checks if a JWT token has expired
 * @param decoded - Decoded JWT payload
 * @returns true if the token has expired, false otherwise
 */
export function isTokenExpired(decoded: JwtPayload): boolean {
  // Convert exp from seconds to milliseconds and compare with current time
  const expirationTime = decoded.exp * 1000;
  return Date.now() >= expirationTime;
}

/**
 * Extracts user context from a JWT token string
 * This is the main function required by US-AUTH-002
 * 
 * @param token - JWT token string or null
 * @returns UserContext with user information or default empty context
 */
export function getUserContextFromToken(token: string | null): UserContext {
  if (!token) {
    return {
      userId: '',
      organizacionId: 0,
      roles: [],
      isAuthenticated: false,
    };
  }

  try {
    const decoded = jwtDecode<JwtPayload>(token);

    // Check if token is expired
    if (isTokenExpired(decoded)) {
      // Clean up localStorage if token is expired
      localStorage.removeItem('auth-storage');
      return {
        userId: '',
        organizacionId: 0,
        roles: [],
        isAuthenticated: false,
      };
    }

    return {
      userId: decoded.sub,
      organizacionId: decoded.organizacion_id,
      roles: decoded.roles,
      isAuthenticated: true,
    };
  } catch (error) {
    // Token is malformed or invalid - clean up localStorage
    console.error('Failed to decode JWT token:', error);
    localStorage.removeItem('auth-storage');
    return {
      userId: '',
      organizacionId: 0,
      roles: [],
      isAuthenticated: false,
    };
  }
}
