/**
 * AclDocumentoSection Component
 * Feature integration component combining modal, list, and state management
 * Complete ACL management interface for document permissions
 */

import React, { useState, useMemo } from 'react';
import AclDocumentoModal from './AclDocumentoModal';
import AclDocumentoList from './AclDocumentoList';
import { useAclDocumento } from '../hooks/useAclDocumento';
import type { IUsuario, IAclDocumento } from '../types';

/**
 * Props for AclDocumentoSection component
 */
export interface AclDocumentoSectionProps {
  /** Document ID for which to manage ACLs */
  documentoId: number;

  /** List of available users to assign permissions to */
  users: IUsuario[];

  /** Whether current user can manage permissions (admin or ADMINISTRACION on parent folder) */
  canManage?: boolean;

  /** Error message to display (e.g., if users failed to load) */
  loadingError?: string | null;

  /** Callback fired when new ACL is created successfully */
  onAclCreated?: (acl: IAclDocumento) => void;

  /** Callback fired when ACL is updated successfully */
  onAclUpdated?: (acl: IAclDocumento) => void;

  /** Callback fired when ACL is revoked successfully */
  onAclRevoked?: (usuarioId: number) => void;

  /** Custom className */
  className?: string;
}

/**
 * AclDocumentoSection Component
 *
 * Complete feature integration component that orchestrates:
 * - Loading ACLs from API
 * - Rendering the ACL list
 * - Managing modal state for create/edit operations
 * - Handling CRUD operations (with upsert behavior)
 * - Admin-only controls
 * - Error handling and user feedback
 *
 * Features:
 * - Auto-loads ACLs on mount
 * - Modal for creating and editing permissions
 * - Table of existing permissions with expiration dates
 * - Admin-only controls
 * - Comprehensive error handling
 * - Callbacks for parent component integration
 * - Upsert behavior (create or update via same endpoint)
 *
 * @component
 * @example
 * <AclDocumentoSection
 *   documentoId={123}
 *   users={users}
 *   canManage={true}
 *   onAclCreated={(acl) => console.log('Created:', acl)}
 * />
 */
export const AclDocumentoSection: React.FC<AclDocumentoSectionProps> = ({
  documentoId,
  users,
  canManage = false,
  loadingError,
  onAclCreated,
  onAclUpdated,
  onAclRevoked,
  className = '',
}) => {
  // State management
  const {
    acls,
    loading,
    error: aclError,
    createOrUpdateAcl,
    revokeAcl,
  } = useAclDocumento(documentoId);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingAcl, setEditingAcl] = useState<IAclDocumento | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [deletingUserIds, setDeletingUserIds] = useState<number[]>([]);

  // Get assigned user IDs
  const assignedUserIds = useMemo(() => {
    return acls.map((acl) => acl.usuario_id);
  }, [acls]);

  /**
   * Open modal for creating new ACL
   */
  const handleCreateClick = () => {
    setEditingAcl(null);
    setSubmitError(null);
    setIsModalOpen(true);
  };

  /**
   * Open modal for editing existing ACL
   */
  const handleEdit = (acl: IAclDocumento) => {
    setEditingAcl(acl);
    setSubmitError(null);
    setIsModalOpen(true);
  };

  /**
   * Handle form submission (create or update via upsert)
   */
  const handleModalSubmit = async (payload: any) => {
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const resultAcl = await createOrUpdateAcl(payload);

      // Determine if it was an update or create based on editingAcl
      if (editingAcl) {
        onAclUpdated?.(resultAcl);
      } else {
        onAclCreated?.(resultAcl);
      }

      // Close modal on success
      setIsModalOpen(false);
      setEditingAcl(null);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Error al guardar permiso';
      setSubmitError(message);
      throw error;
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * Handle revocation (deletion) of ACL
   */
  const handleDelete = async (usuarioId: number) => {
    setDeletingUserIds((prev) => [...prev, usuarioId]);

    try {
      await revokeAcl(usuarioId);
      onAclRevoked?.(usuarioId);
    } catch (error) {
      // Error handling is done by hook
      throw error;
    } finally {
      setDeletingUserIds((prev) =>
        prev.filter((id) => id !== usuarioId)
      );
    }
  };

  // Combine errors
  const combinedError = loadingError || aclError;

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Section header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">
            Permisos de Acceso al Documento
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            Gestiona quién puede acceder a este documento
          </p>
        </div>

        {/* Grant permission button (admin/ADMINISTRACION only) */}
        {canManage && (
          <button
            type="button"
            onClick={handleCreateClick}
            className="inline-flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
          >
            <svg
              className="w-4 h-4 mr-2"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z"
                clipRule="evenodd"
              />
            </svg>
            Otorgar Permiso
          </button>
        )}
      </div>

      {/* ACL list */}
      <AclDocumentoList
        acls={acls}
        onEdit={handleEdit}
        onDelete={handleDelete}
        loading={loading}
        error={combinedError}
        deletingUserIds={deletingUserIds}
        canManage={canManage}
      />

      {/* Modal for create/edit */}
      {canManage && (
        <AclDocumentoModal
          isOpen={isModalOpen}
          onClose={() => {
            setIsModalOpen(false);
            setEditingAcl(null);
            setSubmitError(null);
          }}
          onSubmit={handleModalSubmit}
          users={users}
          assignedUserIds={assignedUserIds}
          editingAcl={editingAcl}
          isSubmitting={isSubmitting}
          submitError={submitError}
          title={
            editingAcl
              ? `Actualizar Permiso: ${editingAcl.usuario.nombre}`
              : 'Otorgar Permiso de Documento'
          }
        />
      )}
    </div>
  );
};

export default AclDocumentoSection;
