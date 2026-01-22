import { apiClient } from '@core/shared/api/axiosInstance';
import { API_ENDPOINTS } from '../constants/messages';
import type { LoginResponse } from '@core/domain/auth/types';

/**
 * Authentication API service
 * Handles communication with the IAM backend service
 */
export const authApi = {
  /**
   * Login with email and password
   * @param email - User email
   * @param password - User password
   * @returns Login response with JWT token
   */
  login: async (email: string, password: string): Promise<LoginResponse> => {
    const { data } = await apiClient.post<LoginResponse>(
      API_ENDPOINTS.AUTH.LOGIN,
      {
        email,
        password,
      }
    );
    return data;
  },

  /**
   * Switch organization context
   * @param organizacionId - New organization ID
   * @returns New JWT token with updated organization context
   */
  switchOrganization: async (organizacionId: number): Promise<LoginResponse> => {
    const { data } = await apiClient.post<LoginResponse>(
      API_ENDPOINTS.AUTH.SWITCH,
      {
        organizacionId,
      }
    );
    return data;
  },
};
