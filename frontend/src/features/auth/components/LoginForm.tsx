import { useEffect, useRef } from 'react';
import { Input, Button, FormField } from '../../../common/ui/forms';
import { useLoginForm } from '../hooks/useLoginForm';

/**
 * Login form component
 * Handles user authentication with email and password
 */
export const LoginForm = () => {
  const {
    formData,
    errors,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit,
  } = useLoginForm();

  const emailInputRef = useRef<HTMLInputElement>(null);

  // Auto-focus email input on mount
  useEffect(() => {
    emailInputRef.current?.focus();
  }, []);

  return (
    <form onSubmit={handleSubmit} noValidate>
      <FormField
        label="Email"
        error={errors.email}
        required
        htmlFor="email"
      >
        <Input
          ref={emailInputRef}
          id="email"
          name="email"
          type="email"
          value={formData.email}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.email}
          disabled={isSubmitting}
          placeholder="usuario@ejemplo.com"
          autoComplete="email"
          required
        />
      </FormField>

      <FormField
        label="Contraseña"
        error={errors.contrasena}
        required
        htmlFor="contrasena"
      >
        <Input
          id="contrasena"
          name="contrasena"
          type="password"
          value={formData.contrasena}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.contrasena}
          disabled={isSubmitting}
          placeholder="••••••••"
          autoComplete="current-password"
          required
        />
      </FormField>

      <Button
        type="submit"
        loading={isSubmitting}
        disabled={isSubmitting}
        variant="primary"
      >
        {isSubmitting ? 'Iniciando sesión...' : 'Iniciar Sesión'}
      </Button>
    </form>
  );
};
