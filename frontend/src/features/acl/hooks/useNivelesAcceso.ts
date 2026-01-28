/**
 * Custom React hook for managing access levels (niveles de acceso)
 * Handles fetching, caching, and state management for ACL data
 * Provides caching mechanism to reduce unnecessary API calls
 */

import { useState, useEffect, useCallback } from 'react';
import { aclApi } from '../services/nivelAccesoService';
import type { INivelAcceso } from '../types';

/**
 * Interface for the hook's return value
 */
export interface UseNivelesAccesoReturn {
  /** Array of access levels (empty while loading) */
  niveles: INivelAcceso[];

  /** Loading state indicator */
  loading: boolean;

  /** Error message (null if no error) */
  error: string | null;

  /** Function to manually refetch data and clear cache */
  refetch: () => Promise<void>;
}

/**
 * Cache key for localStorage
 * Used to persist access levels across browser sessions
 */
const CACHE_KEY = 'ACL_NIVELES_CACHE';

/**
 * Cache entry structure
 * Stores the data and timestamp for TTL validation
 */
interface CacheEntry {
  data: INivelAcceso[];
  timestamp: number;
}

/**
 * Check if cached data is still valid based on TTL
 * @param cacheEntry Cached data with timestamp
 * @param ttl Time-to-live in milliseconds
 * @returns true if cache is still valid, false if expired
 */
function isCacheValid(cacheEntry: CacheEntry, ttl: number): boolean {
  const now = Date.now();
  return now - cacheEntry.timestamp < ttl;
}

/**
 * Custom hook to fetch and manage access levels with caching
 *
 * Features:
 * - Automatic fetching on mount
 * - In-memory and localStorage caching with configurable TTL
 * - Graceful error handling with user-friendly messages
 * - Manual refetch capability with cache invalidation
 * - Optimized to prevent unnecessary API calls
 *
 * @param enableCache - Enable caching mechanism (default: true)
 * @param cacheTTL - Cache time-to-live in milliseconds (default: 24 hours)
 * @returns Object with niveles, loading, error, and refetch
 *
 * @example
 * const { niveles, loading, error, refetch } = useNivelesAcceso();
 *
 * if (loading) return <p>Cargando...</p>;
 * if (error) return <p>Error: {error}</p>;
 *
 * return (
 *   <select>
 *     {niveles.map(nivel => (
 *       <option key={nivel.id} value={nivel.codigo}>
 *         {nivel.nombre}
 *       </option>
 *     ))}
 *   </select>
 * );
 */
export const useNivelesAcceso = (
  enableCache: boolean = true,
  cacheTTL: number = 24 * 60 * 60 * 1000 // 24 hours default
): UseNivelesAccesoReturn => {
  const [niveles, setNiveles] = useState<INivelAcceso[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch access levels from API
   * Handles cache checking and API calls
   */
  const fetchNiveles = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Check cache first if enabled
      if (enableCache) {
        const cached = localStorage.getItem(CACHE_KEY);
        if (cached) {
          try {
            const cacheEntry: CacheEntry = JSON.parse(cached);
            if (isCacheValid(cacheEntry, cacheTTL)) {
              setNiveles(cacheEntry.data);
              setLoading(false);
              return;
            }
          } catch (parseError) {
            // Invalid cache format, proceed with API call
            localStorage.removeItem(CACHE_KEY);
          }
        }
      }

      // Fetch from API
      const data = await aclApi.getNivelesAcceso();

      // Validate response
      if (!Array.isArray(data)) {
        throw new Error('Respuesta inválida del servidor');
      }

      setNiveles(data);

      // Cache the result if enabled
      if (enableCache) {
        const cacheEntry: CacheEntry = {
          data,
          timestamp: Date.now(),
        };
        localStorage.setItem(CACHE_KEY, JSON.stringify(cacheEntry));
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error
          ? err.message
          : 'Error desconocido al cargar niveles de acceso';
      setError(errorMessage);
      setNiveles([]);
    } finally {
      setLoading(false);
    }
  }, [enableCache, cacheTTL]);

  /**
   * Clear cache and refetch data
   * Useful for manual cache invalidation when needed
   */
  const refetch = useCallback(async () => {
    if (enableCache) {
      localStorage.removeItem(CACHE_KEY);
    }
    await fetchNiveles();
  }, [enableCache, fetchNiveles]);

  /**
   * Fetch on component mount
   */
  useEffect(() => {
    fetchNiveles();
  }, [fetchNiveles]);

  return {
    niveles,
    loading,
    error,
    refetch,
  };
};
