/**
 * AclDocumentoList Component
 * Displays document ACLs in a table with edit and delete actions
 * Includes loading, error, empty states, and expiration date display
 */

import React, { useState } from 'react';
import PermissionBadge from './PermissionBadge';
import type { IAclDocumento } from '../types';

/**
 * Props for AclDocumentoList component
 */
export interface AclDocumentoListProps {
  /** Array of ACL records to display */
  acls: IAclDocumento[];

  /** Callback when edit button is clicked */
  onEdit: (acl: IAclDocumento) => void;

  /** Callback when delete button is clicked */
  onDelete: (usuarioId: number) => Promise<void>;

  /** Loading state for initial data fetch */
  loading?: boolean;

  /** Error message to display */
  error?: string | null;

  /** User IDs currently being deleted (shows spinner) */
  deletingUserIds?: number[];

  /** Whether current user can manage (edit/delete) permissions */
  canManage?: boolean;

  /** Custom className */
  className?: string;
}

/**
 * AclDocumentoList Component
 *
 * Displays document ACLs in a responsive table with actions.
 * Shows loading states, errors, empty states, deletion confirmation, and expiration dates.
 *
 * Features:
 * - Responsive table design
 * - Edit and delete actions
 * - Deletion confirmation popup
 * - Loading spinners for async operations
 * - Timestamp formatting in Spanish locale
 * - Expiration date display with visual indicators (expired/expiring soon)
 * - Empty state message
 * - Error display
 * - Hover effects for better UX
 *
 * @component
 * @example
 * <AclDocumentoList
 *   acls={acls}
 *   onEdit={(acl) => setEditingAcl(acl)}
 *   onDelete={(userId) => service.revokeAcl(userId)}
 *   loading={loading}
 *   error={error}
 *   deletingUserIds={deletingIds}
 * />
 */
export const AclDocumentoList: React.FC<AclDocumentoListProps> = ({
  acls,
  onEdit,
  onDelete,
  loading = false,
  error,
  deletingUserIds = [],
  canManage = false,
  className = '',
}) => {
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  /**
   * Format date to Spanish locale
   */
  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateString;
    }
  };

  /**
   * Check if permission is expired or expiring soon
   * Returns: 'expired', 'expiring-soon' (within 7 days), or null
   */
  const getExpirationStatus = (
    fechaExpiracion: string | null
  ): 'expired' | 'expiring-soon' | null => {
    if (!fechaExpiracion) return null;

    const now = new Date();
    const expirationDate = new Date(fechaExpiracion);

    if (expirationDate < now) {
      return 'expired';
    }

    const daysUntilExpiration =
      (expirationDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    if (daysUntilExpiration <= 7) {
      return 'expiring-soon';
    }

    return null;
  };

  /**
   * Handle delete action with confirmation
   */
  const handleDeleteConfirm = async (usuarioId: number) => {
    setDeleteError(null);
    try {
      await onDelete(usuarioId);
      setConfirmDelete(null);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : 'Error al revocar permiso';
      setDeleteError(message);
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <svg
          className="w-8 h-8 animate-spin text-blue-600"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          aria-hidden="true"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
        <span className="ml-2 text-gray-600">Cargando permisos...</span>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div
        className="rounded-md bg-red-50 p-4 border border-red-200"
        role="alert"
      >
        <p className="text-sm font-medium text-red-800">{error}</p>
      </div>
    );
  }

  // Empty state
  if (acls.length === 0) {
    return (
      <div className="text-center py-12">
        <svg
          className="w-12 h-12 text-gray-400 mx-auto mb-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
          />
        </svg>
        <p className="text-gray-500 mb-2">No hay permisos asignados</p>
        <p className="text-sm text-gray-400">
          Comienza otorgando permisos a usuarios
        </p>
      </div>
    );
  }

  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className="min-w-full divide-y divide-gray-200 border border-gray-200 rounded-lg overflow-hidden">
        <thead className="bg-gray-50">
          <tr>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider"
            >
              Usuario
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider"
            >
              Nivel de Acceso
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider"
            >
              Expiración
            </th>
            <th
              scope="col"
              className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider"
            >
              Asignado
            </th>
            {canManage && (
              <th
                scope="col"
                className="px-6 py-3 text-right text-xs font-medium text-gray-700 uppercase tracking-wider"
              >
                Acciones
              </th>
            )}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {acls.map((acl) => {
            const isDeleting = deletingUserIds.includes(acl.usuario_id);
            const expirationStatus = getExpirationStatus(acl.fecha_expiracion);

            return (
              <tr
                key={acl.id}
                className="hover:bg-gray-50 transition-colors"
              >
                {/* Usuario */}
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-xs font-semibold text-blue-700 shrink-0">
                      {acl.usuario.nombre
                        .split(' ')
                        .slice(0, 2)
                        .map((p) => p[0].toUpperCase())
                        .join('')}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {acl.usuario.nombre}
                      </p>
                      <p className="text-xs text-gray-500">
                        {acl.usuario.email}
                      </p>
                    </div>
                  </div>
                </td>

                {/* Nivel de Acceso */}
                <td className="px-6 py-4 whitespace-nowrap">
                  <PermissionBadge
                    nivel={acl.nivel_acceso.codigo}
                    showLabel
                  />
                </td>

                {/* Fecha Expiración */}
                <td className="px-6 py-4 whitespace-nowrap">
                  {acl.fecha_expiracion ? (
                    <div className="flex flex-col gap-1">
                      <span
                        className={`text-sm ${
                          expirationStatus === 'expired'
                            ? 'text-red-600 font-medium'
                            : expirationStatus === 'expiring-soon'
                            ? 'text-orange-600 font-medium'
                            : 'text-gray-600'
                        }`}
                      >
                        {formatDate(acl.fecha_expiracion)}
                      </span>
                      {expirationStatus === 'expired' && (
                        <span className="inline-flex items-center rounded-full bg-red-100 text-red-800 border border-red-200 px-2 py-0.5 text-xs font-medium">
                          Expirado
                        </span>
                      )}
                      {expirationStatus === 'expiring-soon' && (
                        <span className="inline-flex items-center rounded-full bg-orange-100 text-orange-800 border border-orange-200 px-2 py-0.5 text-xs font-medium">
                          Expira pronto
                        </span>
                      )}
                    </div>
                  ) : (
                    <span className="text-sm text-gray-500 italic">
                      Permanente
                    </span>
                  )}
                </td>

                {/* Fecha Asignación */}
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {formatDate(acl.fecha_asignacion)}
                </td>

                {/* Actions */}
                {canManage && (
                  <td className="px-6 py-4 whitespace-nowrap text-right">
                    <div className="flex items-center justify-end gap-2">
                      {/* Edit button */}
                      <button
                        type="button"
                        onClick={() => onEdit(acl)}
                        disabled={isDeleting}
                        className="inline-flex items-center px-2.5 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-50 rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        aria-label={`Editar permiso de ${acl.usuario.nombre}`}
                      >
                        <svg
                          className="w-4 h-4"
                          viewBox="0 0 20 20"
                          fill="currentColor"
                          aria-hidden="true"
                        >
                          <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                        </svg>
                        <span className="ml-1">Editar</span>
                      </button>

                      {/* Delete button with confirmation popup */}
                      <div className="relative">
                        <button
                          type="button"
                          onClick={() =>
                            setConfirmDelete(
                              confirmDelete === acl.usuario_id
                                ? null
                                : acl.usuario_id
                            )
                          }
                          disabled={isDeleting}
                          className="inline-flex items-center px-2.5 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                          aria-label={`Revocar permiso de ${acl.usuario.nombre}`}
                        >
                          {isDeleting ? (
                            <>
                              <svg
                                className="w-4 h-4 animate-spin"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                aria-hidden="true"
                              >
                                <circle
                                  className="opacity-25"
                                  cx="12"
                                  cy="12"
                                  r="10"
                                  stroke="currentColor"
                                  strokeWidth="4"
                                />
                                <path
                                  className="opacity-75"
                                  fill="currentColor"
                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                />
                              </svg>
                            </>
                          ) : (
                            <>
                              <svg
                                className="w-4 h-4"
                                viewBox="0 0 20 20"
                                fill="currentColor"
                                aria-hidden="true"
                              >
                                <path
                                  fillRule="evenodd"
                                  d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                                  clipRule="evenodd"
                                />
                              </svg>
                              <span className="ml-1">Revocar</span>
                            </>
                          )}
                        </button>

                        {/* Confirmation popup */}
                        {confirmDelete === acl.usuario_id && (
                          <div className="absolute right-0 mt-1 bg-white border border-gray-300 rounded-md shadow-lg p-2 z-10 whitespace-nowrap">
                            <p className="text-xs text-gray-600 px-2 py-1">
                              ¿Revocar acceso de {acl.usuario.nombre}?
                            </p>
                            <div className="flex gap-1 px-2 py-1">
                              <button
                                type="button"
                                onClick={() =>
                                  handleDeleteConfirm(acl.usuario_id)
                                }
                                className="px-2 py-1 text-xs font-medium bg-red-600 text-white rounded hover:bg-red-700"
                              >
                                Sí
                              </button>
                              <button
                                type="button"
                                onClick={() => setConfirmDelete(null)}
                                className="px-2 py-1 text-xs font-medium bg-gray-300 text-gray-700 rounded hover:bg-gray-400"
                              >
                                No
                              </button>
                            </div>
                            {deleteError && (
                              <p className="text-xs text-red-600 px-2 py-1">
                                {deleteError}
                              </p>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default AclDocumentoList;
