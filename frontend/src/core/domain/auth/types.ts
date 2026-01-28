/**
 * JWT Payload structure returned by the backend
 * Contains standard JWT claims and custom claims for organization and roles
 */
export interface JwtPayload {
  /** User ID (standard JWT 'sub' claim) */
  sub: string;
  /** Organization ID custom claim */
  organizacion_id: number;
  /** Array of role codes assigned to the user in the organization */
  roles: string[];
  /** Issued at timestamp (seconds since epoch) */
  iat: number;
  /** Expiration timestamp (seconds since epoch) */
  exp: number;
}

/**
 * User context extracted from JWT token
 * Used throughout the application to access user information
 */
export interface UserContext {
  /** User ID */
  userId: string;
  /** Organization ID */
  organizacionId: number;
  /** Array of role codes */
  roles: string[];
  /** Whether the user is authenticated */
  isAuthenticated: boolean;
}

/**
 * Login response from the authentication API
 */
export interface LoginResponse {
  /** JWT token string */
  token: string;
  /** Token type (typically "Bearer") */
  tipoToken: string;
  /** Token expiration time in seconds */
  expiraEn: number;
  /** Organization ID for the authenticated session */
  organizacionId: number;
}

/**
 * Login form data structure
 */
export interface LoginFormData {
  /** User email address */
  email: string;
  /** User password (sent as 'contrasena' to backend) */
  contrasena: string;
}

/**
 * Login form validation errors
 */
export interface LoginFormErrors {
  /** Email field error message */
  email?: string;
  /** Password field error message */
  contrasena?: string;
}

/**
 * Reason for logout
 * Used to display appropriate messages to the user
 */
export type LogoutReason = 'manual' | 'expired' | 'unauthorized';
