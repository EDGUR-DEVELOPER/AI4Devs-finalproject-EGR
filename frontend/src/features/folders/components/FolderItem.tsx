/**
 * FolderItem - Tarjeta individual de carpeta o documento
 * Diferenciación visual por tipo, con acciones contextuales
 */
import React, { useMemo } from 'react';
import { DocumentDownloadButton } from '@features/documents/components/DocumentDownloadButton';
import { DocumentDeleteButton } from '@features/documents/components/DocumentDeleteButton';
import { FolderDeleteButton } from './FolderDeleteButton';
import type { ICapabilities } from '@features/acl';
import type { ContentItem } from '../types/folder.types';

interface FolderItemProps {
  item: ContentItem;
  onClick: () => void;
  onDeleteClick: (id: string) => void;
}

export const FolderItem: React.FC<FolderItemProps> = ({
  item,
  onClick,
  onDeleteClick,
}) => {
  const isFolder = isFolderItem(item);
  const canOpen = item.capacidades?.puede_leer ?? false;
  
  const capabilities: ICapabilities = useMemo(() => {
    const canRead = item.capacidades?.puede_leer ?? false;
    const canWrite = item.capacidades?.puede_escribir ?? false;
    const canAdminister = item.capacidades?.puede_administrar ?? false;
    const canDownload = item.capacidades?.puede_descargar ?? false;

    return {
      canRead,
      canWrite,
      canAdminister,
      canUpload: canWrite,
      canDownload,
      canCreateVersion: !isFolder && canWrite,
      canDeleteFolder: isFolder && canAdminister,
      canManagePermissions: isFolder && canAdminister,
      canChangeVersion: !isFolder && canWrite,
    };
  }, [isFolder, item.capacidades]);

  return (
    <div
      data-testid={isFolder ? `folder-item-${item.id}` : `document-item-${item.id}`}
      className={`relative bg-white border border-gray-200 rounded-lg p-4 transition-shadow group ${
        canOpen ? 'hover:shadow-md cursor-pointer' : 'cursor-not-allowed opacity-80'
      }`}
      onClick={() => {
        if (!canOpen) {
          return;
        }
        onClick();
      }}
      onKeyDown={(e) => {
        if (!canOpen) {
          return;
        }
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onClick();
        }
      }}
      role="button"
      tabIndex={0}
      aria-label={`${isFolder ? 'Carpeta' : 'Documento'}: ${item.nombre}`}
      aria-disabled={!canOpen}
    >
      {/* Icono */}
      <div className="flex items-start justify-between">
        <div className="flex items-center space-x-3">
          <div className="text-3xl" aria-hidden="true">
            {isFolder ? '📁' : '📄'}
          </div>
          
          <div className="flex-1 min-w-0">
            <h3 className="text-sm font-medium text-gray-900 truncate">
              {item.nombre}
            </h3>
            
            {/* Info adicional */}
            <p className="text-xs text-gray-500 mt-1">
              {isFolder ? (
                <>
                  {'num_subcarpetas' in item ? item.num_subcarpetas : 0} carpetas, {'num_documentos' in item ? item.num_documentos : 0} documentos
                </>
              ) : (
                <>
                  Versión {'version_actual' in item ? item.version_actual : 0} · {formatDate(('fecha_modificacion' in item && item.fecha_modificacion) ? item.fecha_modificacion : item.fecha_creacion)}
                </>
              )}
            </p>
          </div>
        </div>

        {/* Menú contextual o botones de acción */}
        {isFolder ? (
          <div className="flex items-center gap-1">
            <FolderDeleteButton
              folderId={item.id}
              folderName={item.nombre}
              canDelete={capabilities.canAdminister}
              onDeleteSuccess={() => onDeleteClick(item.id)}
              size="sm"
              variant="ghost"
              showLabel={false}
            />
          </div>
        ) : (
          <div className="flex items-center gap-1">
            <DocumentDownloadButton
              documentId={item.id}
              fileName={item.nombre}
              canDownload={capabilities.canDownload}
              size="sm"
              showLabel={false}
            />
            <DocumentDeleteButton
              documentId={item.id}
              documentName={item.nombre}
              canDelete={capabilities.canWrite || capabilities.canAdminister}
              onDeleteSuccess={() => onDeleteClick(item.id)}
              size="sm"
              variant="ghost"
              showLabel={false}
            />
          </div>
        )}
      </div>

      {/* Badges de permisos (desarrollo/debug) */}
      {import.meta.env.DEV && (
        <div className="flex gap-1 mt-2">
          {item.capacidades?.puede_leer && <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">Leer</span>}
          {item.capacidades?.puede_escribir && (
            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">Escribir</span>
          )}
          {item.capacidades?.puede_administrar && (
            <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">Admin</span>
          )}
        </div>
      )}
    </div>
  );
};

function isFolderItem(item: ContentItem): boolean {
  if (item.tipo === 'carpeta') {
    return true;
  }

  if (item.tipo === 'documento') {
    return false;
  }

  return 'num_subcarpetas' in item || 'num_documentos' in item;
}

/**
 * Formatea fecha ISO a formato humano
 */
function formatDate(isoDate: string): string {
  const date = new Date(isoDate);
  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}
