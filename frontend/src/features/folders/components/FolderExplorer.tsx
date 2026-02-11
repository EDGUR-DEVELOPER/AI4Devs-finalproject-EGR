/**
 * FolderExplorer - Componente principal para navegación por carpetas
 * Orquesta breadcrumb, listado, creación, eliminación y estado vacío
 */
import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useFolderContent } from '../hooks/useFolderContent';
import { useBreadcrumb } from '../hooks/useBreadcrumb';
import { Breadcrumb } from './Breadcrumb';
import { FolderList } from './FolderList';
import { EmptyFolderState } from './EmptyFolderState';
import { CreateFolderModal } from './CreateFolderModal';
import { DeleteFolderDialog } from './DeleteFolderDialog';
import { Button } from '@ui/forms/Button';

interface FolderExplorerProps {
  /**
   * Control externo del modal de creación (opcional)
   * Si no se provee, el componente maneja su propio estado
   */
  isCreateModalOpen?: boolean;
  onCreateModalChange?: (isOpen: boolean) => void;
  /**
   * Ocultar botón de nueva carpeta en header (opcional)
   * Útil cuando se controla desde componente padre
   */
  hideCreateButton?: boolean;
}

export const FolderExplorer: React.FC<FolderExplorerProps> = ({
  isCreateModalOpen: externalIsCreateModalOpen,
  onCreateModalChange,
  hideCreateButton = false,
}) => {
  const { id: folderId } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // Server state
  const { data: content, isLoading, error } = useFolderContent(folderId);
  const { breadcrumb } = useBreadcrumb(folderId);

  // UI state - usa estado externo si se provee, sino interno
  const [internalIsCreateModalOpen, setInternalIsCreateModalOpen] = useState(false);
  const isCreateModalOpen = externalIsCreateModalOpen ?? internalIsCreateModalOpen;
  const setIsCreateModalOpen = onCreateModalChange ?? setInternalIsCreateModalOpen;

  const [deleteTarget, setDeleteTarget] = useState<{ id: string; nombre: string } | null>(null);

  // Handlers
  const handleFolderClick = (id: string) => {
    navigate(`/carpetas/${id}`);
  };

  const handleDeleteClick = (id: string, nombre: string) => {
    setDeleteTarget({ id, nombre });
  };

  const handleBreadcrumbNavigate = (targetId: string | undefined) => {
    if (!targetId) {
      navigate('/carpetas');
    } else {
      navigate(`/carpetas/${targetId}`);
    }
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded-md" role="alert">
        <h3 className="text-lg font-semibold text-red-800 mb-2">Error al cargar carpeta</h3>
        <p className="text-sm text-red-700">
          {error instanceof Error ? error.message : 'Error desconocido'}
        </p>
        <Button
          onClick={() => navigate('/carpetas')}
          variant="secondary"
          fullWidth={false}
        >
          Volver al inicio
        </Button>
      </div>
    );
  }

  // No data (should not happen)
  if (!content) {
    return null;
  }

  const { 
    subcarpetas = [], 
    documentos = [], 
    permisos = { puede_leer: false, puede_escribir: false, puede_administrar: false } 
  } = content;
  const isEmpty = subcarpetas.length === 0 && documentos.length === 0;

  return (
    <div className="container mx-auto px-4 py-6 max-w-7xl">
      {/* Header: Breadcrumb + Botón Nueva carpeta */}
      <div className="flex items-center justify-between mb-6">
        <Breadcrumb
          segments={breadcrumb}
          onNavigate={handleBreadcrumbNavigate}
        />

        {!hideCreateButton && permisos.puede_escribir && (
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            variant="primary"
            fullWidth={false}
          >
            + Nueva carpeta
          </Button>
        )}
      </div>

      {/* Contenido o estado vacío */}
      {isEmpty ? (
        <EmptyFolderState
          canWrite={permisos.puede_escribir}
          onCreateClick={() => setIsCreateModalOpen(true)}
        />
      ) : (
        <FolderList
          content={content}
          onFolderClick={handleFolderClick}
          onDeleteClick={(folderId) => {
            const folder = content.subcarpetas.find((f) => f.id === folderId);
            if (folder) {
              handleDeleteClick(folderId, folder.nombre);
            }
          }}
        />
      )}

      {/* Modales */}
      <CreateFolderModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        parentFolderId={folderId || null}
      />

      {deleteTarget && (
        <DeleteFolderDialog
          isOpen={true}
          onClose={() => setDeleteTarget(null)}
          folderId={deleteTarget.id}
          folderName={deleteTarget.nombre}
        />
      )}
    </div>
  );
};
