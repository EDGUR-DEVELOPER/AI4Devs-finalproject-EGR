/**
 * PermissionBadge Component
 * Displays access level with visual styling and color coding
 * Used in ACL tables and permission lists
 */

import React from 'react';
import type { CodigoNivelAcceso } from '../types';

/**
 * Props for PermissionBadge component
 */
export interface PermissionBadgeProps {
  /** Access level code to display */
  nivel: CodigoNivelAcceso;

  /** Show label text or only icon/styling (default: true) */
  showLabel?: boolean;

  /** Additional CSS classes */
  className?: string;
}

/**
 * Mapping of access levels to UI styling
 * Includes color classes and display labels
 */
const PERMISSION_STYLES = {
  LECTURA: {
    bgClass: 'bg-blue-100',
    textClass: 'text-blue-800',
    label: 'Lectura',
    borderClass: 'border border-blue-300',
  },
  ESCRITURA: {
    bgClass: 'bg-amber-100',
    textClass: 'text-amber-800',
    label: 'Escritura',
    borderClass: 'border border-amber-300',
  },
  ADMINISTRACION: {
    bgClass: 'bg-red-100',
    textClass: 'text-red-800',
    label: 'Administración',
    borderClass: 'border border-red-300',
  },
} as const;

/**
 * PermissionBadge Component
 *
 * Renders a colored badge displaying the access level with optional label.
 * Uses consistent color scheme: blue=read, amber=write, red=admin.
 *
 * Features:
 * - Color-coded by permission level
 * - Optional text label
 * - Accessible with aria-label
 * - Lightweight and reusable
 * - Tooltip on hover
 *
 * @component
 * @example
 * <PermissionBadge nivel="LECTURA" showLabel />
 *
 * @example
 * <PermissionBadge nivel="ADMINISTRACION" showLabel={false} />
 */
export const PermissionBadge: React.FC<PermissionBadgeProps> = ({
  nivel,
  showLabel = true,
  className = '',
}) => {
  const style = PERMISSION_STYLES[nivel];

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-sm font-medium ${style.bgClass} ${style.textClass} ${style.borderClass} ${className}`}
      role="status"
      title={`Permiso: ${style.label}`}
      aria-label={`Permiso de ${style.label}`}
    >
      {showLabel && <span>{style.label}</span>}
      {!showLabel && (
        <span className="w-2 h-2 rounded-full bg-current" aria-hidden="true" />
      )}
    </span>
  );
};

export default PermissionBadge;
