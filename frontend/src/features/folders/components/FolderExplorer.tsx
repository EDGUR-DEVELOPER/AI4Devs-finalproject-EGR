/**
 * FolderExplorer - Componente principal para navegación por carpetas
 * Orquesta breadcrumb, listado, creación, eliminación y estado vacío
 */
import React, { useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useFolderContent } from '../hooks/useFolderContent';
import { useBreadcrumb } from '../hooks/useBreadcrumb';
import { Breadcrumb } from './Breadcrumb';
import { FolderList } from './FolderList';
import { EmptyFolderState } from './EmptyFolderState';
import { CreateFolderModal } from './CreateFolderModal';
import { DeleteFolderDialog } from './DeleteFolderDialog';
import { Button } from '@ui/forms/Button';
import { useAuth } from '@features/auth/hooks/useAuth';
import { PermissionAwareButton, usePermissionCapabilities } from '@features/acl';
import type { ICapabilities } from '@features/acl';

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
  const { userId } = useAuth();

  // Server state
  const { data: content, isLoading, error } = useFolderContent(folderId);
  const { breadcrumb } = useBreadcrumb(folderId);

  const parsedUserId = Number(userId);
  const permissionContext = useMemo(
    () => ({
      entityId: Number(folderId ?? 0),
      entityType: 'carpeta' as const,
      usuarioId: Number.isFinite(parsedUserId) ? parsedUserId : 0,
    }),
    [folderId, parsedUserId]
  );

  const permissionState = usePermissionCapabilities(permissionContext);

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
  if (isLoading || (folderId && permissionState.isLoading)) {
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

  const fallbackCapabilities: ICapabilities = {
    canRead: permisos.puede_leer,
    canWrite: permisos.puede_escribir,
    canAdminister: permisos.puede_administrar,
    canUpload: permisos.puede_escribir,
    canDownload: permisos.puede_leer,
    canCreateVersion: permisos.puede_escribir,
    canDeleteFolder: permisos.puede_administrar,
    canManagePermissions: permisos.puede_administrar,
    canChangeVersion: permisos.puede_escribir,
  };

  const capabilities = folderId ? permissionState.capabilities : fallbackCapabilities;

  return (
    <div className="container mx-auto px-4 py-6 max-w-7xl">
      {/* Header: Breadcrumb + Botón Nueva carpeta */}
      <div className="flex items-center justify-between mb-6">
        <Breadcrumb
          segments={breadcrumb}
          onNavigate={handleBreadcrumbNavigate}
        />

        {!hideCreateButton && (
          <PermissionAwareButton
            onClick={() => setIsCreateModalOpen(true)}
            action="crear_carpeta"
            capabilities={capabilities}
            variant="primary"
            fullWidth={false}
          >
            + Nueva carpeta
          </PermissionAwareButton>
        )}
      </div>

      {/* Contenido o estado vacío */}
      {isEmpty ? (
        <EmptyFolderState
          capabilities={capabilities}
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
        canWrite={capabilities.canWrite}
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
