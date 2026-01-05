import { useState } from 'react';
import type { FormEvent, ChangeEvent } from 'react';
import { useAuth } from './useAuth';
import type { LoginFormData, LoginFormErrors } from '../../../core/domain/auth/types';
import { validateEmail, validatePassword, VALIDATION_MESSAGES } from '../constants/validation';

/**
 * Custom hook for managing login form state and validation
 * Handles form data, validation, submission, and error states
 */
export const useLoginForm = () => {
  const { login } = useAuth();
  
  const [formData, setFormData] = useState<LoginFormData>({
    email: '',
    contrasena: '',
  });
  
  const [errors, setErrors] = useState<LoginFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  /**
   * Handles input field changes
   */
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    
    // Clear error for this field when user starts typing
    if (errors[name as keyof LoginFormErrors]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }
  };

  /**
   * Handles input field blur events for on-blur validation
   */
  const handleBlur = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    let error: string | null = null;

    if (name === 'email') {
      error = validateEmail(value);
    } else if (name === 'contrasena') {
      error = validatePassword(value);
    }

    if (error) {
      setErrors((prev) => ({ ...prev, [name]: error }));
    }
  };

  /**
   * Validates the entire form
   * @returns true if form is valid, false otherwise
   */
  const validateForm = (): boolean => {
    const newErrors: LoginFormErrors = {};

    const emailError = validateEmail(formData.email);
    if (emailError) {
      newErrors.email = emailError;
    }

    const passwordError = validatePassword(formData.contrasena);
    if (passwordError) {
      newErrors.contrasena = passwordError;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Handles form submission
   */
  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // Validate form before submission
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      await login(formData.email, formData.contrasena);
      // Login successful - useAuth handles notification and navigation
      // Reset form
      setFormData({ email: '', contrasena: '' });
      setErrors({});
    } catch (error: any) {
      // Handle authentication errors
      if (error.response?.status === 401) {
        setErrors({
          contrasena: VALIDATION_MESSAGES.INVALID_CREDENTIALS,
        });
      } else {
        // Other errors are handled by useAuth with notifications
        console.error('Login error:', error);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    formData,
    errors,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit,
  };
};
