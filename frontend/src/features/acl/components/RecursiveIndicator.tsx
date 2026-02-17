/**
 * RecursiveIndicator Component
 * Displays visual indicator showing if a permission is recursive (applies to subfolders)
 */

import React from 'react';

/**
 * Props for RecursiveIndicator component
 */
export interface RecursiveIndicatorProps {
  /** Whether the permission is recursive */
  recursivo: boolean;

  /** Show label text or only icon (default: true) */
  showLabel?: boolean;

  /** Size variant (default: md) */
  size?: 'sm' | 'md' | 'lg';

  /** Additional CSS classes */
  className?: string;
}

/**
 * Size mapping for icons
 */
const SIZE_CLASSES = {
  sm: 'w-3 h-3',
  md: 'w-4 h-4',
  lg: 'w-5 h-5',
} as const;

/**
 * RecursiveIndicator Component
 *
 * Displays a visual indicator showing whether a folder permission
 * applies recursively to subfolders or is limited to the folder only.
 *
 * Features:
 * - Two visual states: recursive (nested folders) vs direct (single folder)
 * - Color-coded: green for recursive, gray for direct
 * - Optional text label
 * - Accessible with aria-label
 * - Configurable size
 *
 * @component
 * @example
 * <RecursiveIndicator recursivo={true} showLabel />
 *
 * @example
 * <RecursiveIndicator recursivo={false} size="sm" />
 */
export const RecursiveIndicator: React.FC<RecursiveIndicatorProps> = ({
  recursivo,
  showLabel = true,
  size = 'md',
  className = '',
}) => {
  const sizeClass = SIZE_CLASSES[size];

  return (
    <div
      className={`inline-flex items-center gap-1 ${className}`}
      role="status"
      aria-label={recursivo ? 'Permiso recursivo (incluye subcarpetas)' : 'Permiso directo (solo esta carpeta)'}
    >
      {recursivo ? (
        // Nested folders icon (recursive)
        <svg
          className={`${sizeClass} text-green-600`}
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4z" />
          <path d="M3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6z" />
          <path
            fillOpacity="0.5"
            d="M14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"
          />
        </svg>
      ) : (
        // Single folder icon (direct)
        <svg
          className={`${sizeClass} text-gray-400`}
          viewBox="0 0 20 20"
          fill="currentColor"
          aria-hidden="true"
        >
          <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
        </svg>
      )}

      {showLabel && (
        <span className="text-sm font-medium">
          {recursivo ? 'Recursivo' : 'Directo'}
        </span>
      )}
    </div>
  );
};

export default RecursiveIndicator;
