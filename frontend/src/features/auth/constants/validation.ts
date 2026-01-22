/**
 * Email validation regex pattern
 * Validates standard email format
 */
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Minimum password length
 */
export const MIN_PASSWORD_LENGTH = 6;

/**
 * Validation error messages
 * Centralized for consistency and future i18n support
 */
export const VALIDATION_MESSAGES = {
  REQUIRED_FIELD: 'Este campo es obligatorio',
  INVALID_EMAIL_FORMAT: 'Formato de email inválido',
  MIN_PASSWORD_LENGTH: `La contraseña debe tener al menos ${MIN_PASSWORD_LENGTH} caracteres`,
  INVALID_CREDENTIALS: 'Email o contraseña incorrectos',
} as const;

/**
 * Validates email format
 * @param value - Email string to validate
 * @returns Error message or null if valid
 */
export const validateEmail = (value: string): string | null => {
  if (!value || value.trim() === '') {
    return VALIDATION_MESSAGES.REQUIRED_FIELD;
  }
  if (!EMAIL_PATTERN.test(value)) {
    return VALIDATION_MESSAGES.INVALID_EMAIL_FORMAT;
  }
  return null;
};

/**
 * Validates password format
 * @param value - Password string to validate
 * @returns Error message or null if valid
 */
export const validatePassword = (value: string): string | null => {
  if (!value || value.trim() === '') {
    return VALIDATION_MESSAGES.REQUIRED_FIELD;
  }
  if (value.length < MIN_PASSWORD_LENGTH) {
    return VALIDATION_MESSAGES.MIN_PASSWORD_LENGTH;
  }
  return null;
};
