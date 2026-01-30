/**
 * AclCarpetaSection Component
 * Feature integration component combining modal, list, and state management
 * Complete ACL management interface for folder permissions
 */

import React, { useState, useMemo } from 'react';
import AclCarpetaModal from './AclCarpetaModal';
import AclCarpetaList from './AclCarpetaList';
import { useAclCarpeta } from '../hooks/useAclCarpeta';
import type { IUsuario, IAclCarpeta } from '../types';

/**
 * Props for AclCarpetaSection component
 */
export interface AclCarpetaSectionProps {
  /** Folder ID for which to manage ACLs */
  carpetaId: number;

  /** List of available users to assign permissions to */
  users: IUsuario[];

  /** Whether current user is admin (controls visibility of controls) */
  isAdmin?: boolean;

  /** Error message to display (e.g., if users failed to load) */
  loadingError?: string | null;

  /** Callback fired when new ACL is created successfully */
  onAclCreated?: (acl: IAclCarpeta) => void;

  /** Callback fired when ACL is updated successfully */
  onAclUpdated?: (acl: IAclCarpeta) => void;

  /** Callback fired when ACL is deleted successfully */
  onAclDeleted?: (usuarioId: number) => void;

  /** Custom className */
  className?: string;
}

/**
 * AclCarpetaSection Component
 *
 * Complete feature integration component that orchestrates:
 * - Loading ACLs from API
 * - Rendering the ACL list
 * - Managing modal state for create/edit operations
 * - Handling CRUD operations
 * - Admin-only controls
 * - Error handling and user feedback
 *
 * Features:
 * - Auto-loads ACLs on mount
 * - Modal for creating and editing permissions
 * - Table of existing permissions
 * - Admin-only controls
 * - Comprehensive error handling
 * - Callbacks for parent component integration
 *
 * @component
 * @example
 * <AclCarpetaSection
 *   carpetaId={123}
 *   users={users}
 *   isAdmin={true}
 *   onAclCreated={(acl) => console.log('Created:', acl)}
 * />
 */
export const AclCarpetaSection: React.FC<AclCarpetaSectionProps> = ({
  carpetaId,
  users,
  isAdmin = false,
  loadingError,
  onAclCreated,
  onAclUpdated,
  onAclDeleted,
  className = '',
}) => {
  // State management
  const {
    acls,
    loading,
    error: aclError,
    createAcl,
    updateAcl,
    deleteAcl
  } = useAclCarpeta(carpetaId);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingAcl, setEditingAcl] = useState<IAclCarpeta | null>(null);
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
  const handleEdit = (acl: IAclCarpeta) => {
    setEditingAcl(acl);
    setSubmitError(null);
    setIsModalOpen(true);
  };

  /**
   * Handle form submission (create or update)
   */
  const handleModalSubmit = async (payload: any) => {
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      if (editingAcl) {
        // Update mode
        await updateAcl(editingAcl.usuario_id, payload);
        onAclUpdated?.(editingAcl); // Call callback (will be same ACL object from hook)
      } else {
        // Create mode
        await createAcl(payload);
        // Get the newly created ACL from the updated acls array
        // Note: The hook automatically adds it to state
        onAclCreated?.(acls[acls.length - 1]);
      }
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
   * Handle deletion of ACL
   */
  const handleDelete = async (usuarioId: number) => {
    setDeletingUserIds((prev) => [...prev, usuarioId]);

    try {
      await deleteAcl(usuarioId);
      onAclDeleted?.(usuarioId);
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
            Permisos de Acceso
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            Gestiona quién puede acceder a esta carpeta
          </p>
        </div>

        {/* Grant permission button (admin only) */}
        {isAdmin && (
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
      <AclCarpetaList
        acls={acls}
        onEdit={handleEdit}
        onDelete={handleDelete}
        loading={loading}
        error={combinedError}
        deletingUserIds={deletingUserIds}
      />

      {/* Modal for create/edit */}
      {isAdmin && (
        <AclCarpetaModal
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
              : 'Otorgar Permiso de Carpeta'
          }
        />
      )}
    </div>
  );
};

export default AclCarpetaSection;
