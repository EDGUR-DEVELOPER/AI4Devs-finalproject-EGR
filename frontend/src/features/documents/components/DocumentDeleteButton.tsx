/**
 * DocumentDeleteButton Component
 * US-DOC-008: Botón reutilizable para eliminar documento
 */

import React, { useState } from 'react';
import { DocumentDeleteConfirmDialog } from './DocumentDeleteConfirmDialog';
import { useDocumentDelete } from '../hooks/useDocumentDelete';

export interface DocumentDeleteButtonProps {
  /** ID del documento a eliminar */
  documentId: string | number;
  /** Nombre del documento para mostrar en diálogo */
  documentName: string;
  /** ¿Usuario tiene permisos de ESCRITURA o ADMINISTRACION? */
  canDelete: boolean;
  /** Callback tras eliminación exitosa */
  onDeleteSuccess?: () => void;
  /** ¿Deshabilitar botón? */
  disabled?: boolean;
  /** Tamaño del botón */
  size?: 'sm' | 'md' | 'lg';
  /** Estilo del botón */
  variant?: 'danger' | 'ghost' | 'outline';
  /** ¿Mostrar texto "Eliminar"? */
  showLabel?: boolean;
  /** Clases CSS adicionales */
  className?: string;
}

/**
 * Botón reutilizable para eliminar documentos
 * 
 * Orquestra diálogo de confirmación + hook de eliminación.
 * No se renderiza si usuario no tiene permisos (canDelete=false).
 * 
 * @component
 * @example
 * <DocumentDeleteButton
 *   documentId="doc-123"
 *   documentName="contrato.pdf"
 *   canDelete={true}
 *   onDeleteSuccess={() => refreshList()}
 *   size="sm"
 *   variant="ghost"
 *   showLabel={false}
 * />
 */
export const DocumentDeleteButton: React.FC<DocumentDeleteButtonProps> = ({
  documentId,
  documentName,
  canDelete,
  onDeleteSuccess,
  disabled = false,
  size = 'md',
  variant = 'danger',
  showLabel = true,
  className = '',
}) => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const { isDeleting, deleteDocumentWithConfirmation } = useDocumentDelete();

  // No renderizar si no tiene permisos
  if (!canDelete) {
    return null;
  }

  // Tamaños de botón
  const sizeClasses = {
    sm: 'px-2 py-1 text-xs',
    md: 'px-3 py-2 text-sm',
    lg: 'px-4 py-2.5 text-base',
  };

  // Variantes de estilo
  const variantClasses = {
    danger: 'text-red-600 hover:bg-red-50 hover:text-red-700',
    ghost: 'text-gray-600 hover:bg-gray-100 hover:text-gray-700',
    outline: 'text-red-600 border border-red-200 hover:bg-red-50',
  };

  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsDialogOpen(true);
  };

  const handleConfirmDelete = async () => {
    const success = await deleteDocumentWithConfirmation(documentId, documentName);
    if (success) {
      setIsDialogOpen(false);
      if (onDeleteSuccess) {
        onDeleteSuccess();
      }
    }
  };

  const handleCancelDelete = () => {
    setIsDialogOpen(false);
  };

  // Icono papelera (SVG)
  const TrashIcon = () => (
    <svg
      className="h-4 w-4"
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 20 20"
      fill="currentColor"
    >
      <path
        fillRule="evenodd"
        d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
        clipRule="evenodd"
      />
    </svg>
  );

  return (
    <>
      <button
        onClick={handleDeleteClick}
        onMouseDown={(e) => e.stopPropagation()}
        disabled={disabled || isDeleting}
        className={`
          inline-flex items-center justify-center gap-2
          rounded-lg transition-colors font-medium
          focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500
          disabled:opacity-50 disabled:cursor-not-allowed
          ${sizeClasses[size]}
          ${variantClasses[variant]}
          ${className}
        `.trim()}
        title="Eliminar documento"
        aria-label={`Eliminar ${documentName}`}
        type="button"
      >
        <TrashIcon />
        {showLabel && 'Eliminar'}
      </button>

      <DocumentDeleteConfirmDialog
        isOpen={isDialogOpen}
        onClose={handleCancelDelete}
        onConfirm={handleConfirmDelete}
        documentName={documentName}
        isDeleting={isDeleting}
      />
    </>
  );
};
