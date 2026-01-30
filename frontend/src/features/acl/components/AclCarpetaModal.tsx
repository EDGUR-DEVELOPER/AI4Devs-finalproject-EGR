/**
 * AclCarpetaModal Component
 * Modal dialog for creating and updating folder ACLs
 * Includes form validation, error handling, and loading states
 */

import React, { useState, useEffect } from 'react';
import UserSelect from './UserSelect';
import NivelAccesoSelect from './NivelAccesoSelect';
import type {
  IUsuario,
  IAclCarpeta,
  CreateAclCarpetaDTO,
  UpdateAclCarpetaDTO,
  CodigoNivelAcceso,
} from '../types';

/**
 * Props for AclCarpetaModal component
 */
export interface AclCarpetaModalProps {
  /** Whether modal is open */
  isOpen: boolean;

  /** Close modal callback */
  onClose: () => void;

  /** Submit callback for new ACL */
  onSubmit: (payload: CreateAclCarpetaDTO | UpdateAclCarpetaDTO) => Promise<void>;

  /** List of available users */
  users: IUsuario[];

  /** User IDs already assigned to this folder */
  assignedUserIds: number[];

  /** ACL being edited (null = create mode) */
  editingAcl: IAclCarpeta | null;

  /** Whether submit is in progress */
  isSubmitting?: boolean;

  /** Submit error message */
  submitError?: string | null;

  /** Modal title */
  title?: string;

  /** Custom className */
  className?: string;
}

/**
 * Internal form state
 */
interface FormState {
  usuarioId: number | null;
  nivelAccesoCodigo: CodigoNivelAcceso | '';
  recursivo: boolean;
  comentarioOpcional: string;
}

/**
 * AclCarpetaModal Component
 *
 * Modal dialog for creating or updating folder ACLs.
 * Provides form validation, error handling, and submission management.
 *
 * Features:
 * - Two modes: create (empty form) and edit (pre-filled form)
 * - Form validation with error messages
 * - User selection with exclusion of already-assigned users
 * - Recursive permission toggle
 * - Loading states and disabled submit during submission
 * - Keyboard navigation (Esc to close)
 * - Backdrop to prevent interaction with page content
 *
 * @component
 * @example
 * <AclCarpetaModal
 *   isOpen={isModalOpen}
 *   onClose={() => setIsModalOpen(false)}
 *   onSubmit={handleSubmit}
 *   users={users}
 *   assignedUserIds={[1, 2, 3]}
 *   editingAcl={null}
 * />
 */
export const AclCarpetaModal: React.FC<AclCarpetaModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  users,
  assignedUserIds,
  editingAcl,
  isSubmitting = false,
  submitError,
  title,
  className = '',
}) => {
  const [form, setForm] = useState<FormState>({
    usuarioId: null,
    nivelAccesoCodigo: '',
    recursivo: false,
    comentarioOpcional: '',
  });

  const [formError, setFormError] = useState<string | null>(null);

  const isEditMode = editingAcl !== null;
  const modalTitle = title ||
    (isEditMode ? 'Actualizar Permiso de Carpeta' : 'Otorgar Permiso de Carpeta');

  // Initialize form when modal opens or editing ACL changes
  useEffect(() => {
    if (isOpen) {
      if (isEditMode && editingAcl) {
        setForm({
          usuarioId: editingAcl.usuario_id,
          nivelAccesoCodigo: editingAcl.nivel_acceso.codigo,
          recursivo: editingAcl.recursivo,
          comentarioOpcional: '',
        });
      } else {
        setForm({
          usuarioId: null,
          nivelAccesoCodigo: '',
          recursivo: false,
          comentarioOpcional: '',
        });
      }
      setFormError(null);
    }
  }, [isOpen, isEditMode, editingAcl]);

  // Validate form
  const validateForm = (): boolean => {
    if (!form.usuarioId) {
      setFormError('Debes seleccionar un usuario');
      return false;
    }
    if (!form.nivelAccesoCodigo) {
      setFormError('Debes seleccionar un nivel de acceso');
      return false;
    }
    setFormError(null);
    return true;
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      const selectedUser = users.find((u) => u.id === form.usuarioId);
      if (!selectedUser) {
        setFormError('Usuario no válido');
        return;
      }

      const payload = isEditMode
        ? ({
            nivel_acceso_codigo: form.nivelAccesoCodigo as CodigoNivelAcceso,
            recursivo: form.recursivo,
          } as UpdateAclCarpetaDTO)
        : ({
            usuario_id: form.usuarioId,
            nivel_acceso_codigo: form.nivelAccesoCodigo as CodigoNivelAcceso,
            recursivo: form.recursivo,
            comentario_opcional: form.comentarioOpcional || undefined,
          } as CreateAclCarpetaDTO);

      await onSubmit(payload);
      onClose();
    } catch (error) {
      // Error is handled by parent component and shown in submitError
    }
  };

  // Handle keyboard events
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape' && !isSubmitting) {
      onClose();
    }
  };

  if (!isOpen) {
    return null;
  }

  // Determine which user IDs to exclude from dropdown
  // In edit mode, exclude all assigned users EXCEPT the one being edited
  const excludeIds = isEditMode
    ? assignedUserIds.filter((id) => id !== editingAcl?.usuario_id)
    : assignedUserIds;

  const selectedUser = form.usuarioId
    ? users.find((u) => u.id === form.usuarioId) || null
    : null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40 transition-opacity"
        onClick={() => !isSubmitting && onClose()}
        aria-hidden="true"
      />

      {/* Modal */}
      <div
        className={`fixed inset-0 z-50 flex items-center justify-center p-4 ${className}`}
        onKeyDown={handleKeyDown}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 id="modal-title" className="text-lg font-semibold text-gray-900">
              {modalTitle}
            </h2>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
              aria-label="Cerrar modal"
            >
              <svg
                className="w-6 h-6"
                viewBox="0 0 20 20"
                fill="currentColor"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                  clipRule="evenodd"
                />
              </svg>
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            {/* Error alert */}
            {(formError || submitError) && (
              <div
                className="rounded-md bg-red-50 p-4 border border-red-200"
                role="alert"
              >
                <p className="text-sm text-red-800 font-medium">
                  {formError || submitError}
                </p>
              </div>
            )}

            {/* User select */}
            <UserSelect
              users={users}
              value={selectedUser}
              onChange={(user) =>
                setForm((prev) => ({ ...prev, usuarioId: user.id }))
              }
              excludeUserIds={excludeIds}
              label="Usuario"
              required
              error={formError?.includes('usuario') ? formError : null}
              disabled={isEditMode || isSubmitting}
              showSearch
            />

            {/* Access level select */}
            <NivelAccesoSelect
              value={form.nivelAccesoCodigo}
              onChange={(codigo) =>
                setForm((prev) => ({ ...prev, nivelAccesoCodigo: codigo }))
              }
              label="Nivel de Acceso"
              required
              error={formError?.includes('nivel') ? formError : null}
              disabled={isSubmitting}
            />

            {/* Recursive toggle */}
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={form.recursivo}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, recursivo: e.target.checked }))
                }
                disabled={isSubmitting}
                className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700">
                Aplicar recursivamente a subcarpetas
              </span>
            </label>

            {/* Comment field (create mode only) */}
            {!isEditMode && (
              <div>
                <label
                  htmlFor="comment"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Comentario (opcional)
                </label>
                <textarea
                  id="comment"
                  value={form.comentarioOpcional}
                  onChange={(e) =>
                    setForm((prev) => ({
                      ...prev,
                      comentarioOpcional: e.target.value,
                    }))
                  }
                  disabled={isSubmitting}
                  placeholder="Ej: Acceso para auditoría..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-100"
                  rows={3}
                />
              </div>
            )}
          </form>

          {/* Footer */}
          <div className="flex gap-3 px-6 py-4 bg-gray-50 border-t border-gray-200 rounded-b-lg">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              onClick={handleSubmit}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isSubmitting && (
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
              )}
              {isEditMode ? 'Actualizar' : 'Otorgar'} Permiso
            </button>
          </div>
        </div>
      </div>
    </>
  );
};

export default AclCarpetaModal;
