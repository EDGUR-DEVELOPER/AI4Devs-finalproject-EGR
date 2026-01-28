/**
 * NivelAccesoSelect Component
 * Dropdown selector for access levels
 * Uses the useNivelesAcceso hook to fetch and manage access levels
 */

import React, { useMemo } from 'react';
import { useNivelesAcceso } from '../hooks/useNivelesAcceso';
import type { INivelAcceso, CodigoNivelAcceso } from '../types';

/**
 * Props for NivelAccesoSelect component
 */
export interface NivelAccesoSelectProps {
  /** Currently selected access level code */
  value: CodigoNivelAcceso | '';

  /** Callback when selection changes */
  onChange: (codigo: CodigoNivelAcceso) => void;

  /** Label text displayed above the select (optional) */
  label?: string;

  /** Disable the select input */
  disabled?: boolean;

  /** Mark the field as required (shows asterisk) */
  required?: boolean;

  /** Error message to display below the select */
  error?: string | null;

  /** CSS size class (sm, md, lg) */
  size?: 'sm' | 'md' | 'lg';

  /** Additional CSS classes */
  className?: string;

  /** Placeholder text when no option is selected */
  placeholder?: string;

  /** Include a "Seleccionar..." default option */
  showDefaultOption?: boolean;
}

/**
 * NivelAccesoSelect Component
 *
 * Provides a dropdown selector for choosing access levels.
 * Handles loading states, errors, and integrates with the useNivelesAcceso hook.
 *
 * Features:
 * - Automatic loading and caching of access levels
 * - Loading and error state handling
 * - Sorting by order attribute
 * - Accessible labels and error messages
 * - Customizable size and styling
 * - Keyboard navigation support
 *
 * @component
 * @example
 * const [selectedLevel, setSelectedLevel] = useState<CodigoNivelAcceso | ''>('');
 *
 * <NivelAccesoSelect
 *   value={selectedLevel}
 *   onChange={setSelectedLevel}
 *   label="Nivel de Acceso"
 *   required
 * />
 *
 * @example
 * // With error handling
 * <NivelAccesoSelect
 *   value={selectedLevel}
 *   onChange={setSelectedLevel}
 *   label="Nivel de Acceso"
 *   error={validationError}
 *   required
 * />
 */
export const NivelAccesoSelect: React.FC<NivelAccesoSelectProps> = ({
  value,
  onChange,
  label,
  disabled = false,
  required = false,
  error = null,
  size = 'md',
  className = '',
  placeholder = 'Seleccionar nivel de acceso...',
  showDefaultOption = true,
}) => {
  const { niveles, loading, error: fetchError, refetch } = useNivelesAcceso();

  /**
   * Sort levels by order attribute for consistent UI presentation
   */
  const sortedNiveles = useMemo(() => {
    return [...niveles].sort((a, b) => a.orden - b.orden);
  }, [niveles]);

  /**
   * Map size prop to Tailwind CSS classes
   */
  const sizeClasses = useMemo(() => {
    switch (size) {
      case 'sm':
        return 'py-1 px-2 text-sm';
      case 'lg':
        return 'py-3 px-4 text-lg';
      case 'md':
      default:
        return 'py-2 px-3 text-base';
    }
  }, [size]);

  /**
   * Combine error states: validation error takes precedence
   */
  const displayError = error || fetchError;

  /**
   * Handle selection change
   */
  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const selectedCode = e.target.value as CodigoNivelAcceso;
    if (selectedCode) {
      onChange(selectedCode);
    }
  };

  /**
   * Unique ID for label and input association
   */
  const selectId = `nivel-acceso-select-${Math.random().toString(36).slice(2, 9)}`;

  return (
    <div className={`w-full ${className}`}>
      {/* Label */}
      {label && (
        <label
          htmlFor={selectId}
          className="block mb-1 text-sm font-medium text-gray-700"
        >
          {label}
          {required && <span className="ml-1 text-red-500">*</span>}
        </label>
      )}

      {/* Select Input */}
      <div className="relative">
        <select
          id={selectId}
          value={value}
          onChange={handleChange}
          disabled={disabled || loading}
          aria-invalid={!!displayError}
          aria-describedby={displayError ? `${selectId}-error` : undefined}
          className={`
            w-full
            ${sizeClasses}
            border rounded-md
            transition-colors duration-200
            font-sans
            focus:outline-none focus:ring-2 focus:ring-offset-0
            ${
              disabled
                ? 'bg-gray-100 border-gray-300 text-gray-500 cursor-not-allowed'
                : displayError
                  ? 'border-red-500 bg-white text-gray-900 focus:ring-red-500'
                  : 'border-gray-300 bg-white text-gray-900 hover:border-gray-400 focus:ring-blue-500'
            }
            ${loading ? 'opacity-75' : 'opacity-100'}
          `}
        >
          {/* Default/Placeholder Option */}
          {showDefaultOption && (
            <option value="">{placeholder}</option>
          )}

          {/* Loading State */}
          {loading && !sortedNiveles.length && (
            <option disabled>Cargando niveles...</option>
          )}

          {/* Access Level Options */}
          {sortedNiveles.map((nivel: INivelAcceso) => (
            <option
              key={nivel.id}
              value={nivel.codigo}
              disabled={!nivel.activo}
            >
              {nivel.nombre}
              {!nivel.activo && ' (Inactivo)'}
            </option>
          ))}

          {/* Empty State */}
          {!loading && !sortedNiveles.length && (
            <option disabled>No hay niveles disponibles</option>
          )}
        </select>

        {/* Loading Indicator */}
        {loading && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            <div className="inline-flex h-4 w-4 animate-spin rounded-full border-2 border-gray-300 border-t-blue-500" />
          </div>
        )}
      </div>

      {/* Error Message */}
      {displayError && (
        <div className="mt-2 flex items-start gap-2">
          <svg
            className="h-5 w-5 flex-shrink-0 text-red-500"
            fill="currentColor"
            viewBox="0 0 20 20"
            aria-hidden="true"
          >
            <path
              fillRule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z"
              clipRule="evenodd"
            />
          </svg>
          <div className="flex-1">
            <p
              id={`${selectId}-error`}
              className="text-sm text-red-600"
            >
              {displayError}
            </p>
            {fetchError && (
              <button
                onClick={refetch}
                type="button"
                className="mt-1 text-xs font-medium text-red-600 hover:text-red-700 underline"
              >
                Reintentar cargar
              </button>
            )}
          </div>
        </div>
      )}

      {/* Helper Text for Required Field */}
      {!displayError && required && (
        <p className="mt-1 text-xs text-gray-500">Este campo es obligatorio</p>
      )}
    </div>
  );
};

export default NivelAccesoSelect;
