/**
 * FolderItem - Tarjeta individual de carpeta o documento
 * Diferenciación visual por tipo, con acciones contextuales
 */
import React, { useMemo } from 'react';
import { PermissionAwareMenu } from '@features/acl';
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
  const isFolder = item.tipo === 'carpeta';
  const canOpen = item.puede_leer;
  
  const capabilities: ICapabilities = useMemo(() => {
    const canWrite = 'puede_escribir' in item ? item.puede_escribir : false;
    const canAdminister = 'puede_administrar' in item ? item.puede_administrar : false;
    const canDownload = 'puede_descargar' in item ? item.puede_descargar : false;

    return {
      canRead: item.puede_leer,
      canWrite,
      canAdminister,
      canUpload: canWrite,
      canDownload,
      canCreateVersion: !isFolder && canWrite,
      canDeleteFolder: isFolder && canAdminister,
      canManagePermissions: isFolder && canAdminister,
      canChangeVersion: !isFolder && canWrite,
    };
  }, [isFolder, item]);

  const folderActions = useMemo(() => {
    if (!isFolder) {
      return [];
    }

    return [
      {
        id: 'eliminar_carpeta',
        label: 'Eliminar carpeta',
        variant: 'danger' as const,
        onClick: () => onDeleteClick(item.id),
      },
    ];
  }, [isFolder, item.id, onDeleteClick]);

  return (
    <div
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
                  {item.num_subcarpetas || 0} carpetas, {item.num_documentos || 0} documentos
                </>
              ) : (
                <>
                  Versión {item.version_actual} · {formatDate(item.fecha_modificacion)}
                </>
              )}
            </p>
          </div>
        </div>

        {/* Menú contextual */}
        {isFolder && (
          <PermissionAwareMenu
            actions={folderActions}
            capabilities={capabilities}
          />
        )}
      </div>

      {/* Badges de permisos (desarrollo/debug) */}
      {import.meta.env.DEV && (
        <div className="flex gap-1 mt-2">
          {item.puede_leer && <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">Leer</span>}
          {'puede_escribir' in item && item.puede_escribir && (
            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">Escribir</span>
          )}
          {'puede_administrar' in item && item.puede_administrar && (
            <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">Admin</span>
          )}
        </div>
      )}
    </div>
  );
};

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
