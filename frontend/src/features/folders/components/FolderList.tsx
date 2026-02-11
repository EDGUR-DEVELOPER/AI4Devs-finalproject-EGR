/**
 * FolderList - Lista de subcarpetas y documentos
 * Muestra contenido completo con separación visual entre tipos
 */
import React from 'react';
import { FolderItem } from './FolderItem';
import type { FolderContent } from '../types/folder.types';

interface FolderListProps {
  content: FolderContent;
  onFolderClick: (folderId: string) => void;
  onDeleteClick: (folderId: string) => void;
}

export const FolderList: React.FC<FolderListProps> = ({
  content,
  onFolderClick,
  onDeleteClick,
}) => {
  const hasFolders = content.subcarpetas.length > 0;
  const hasDocuments = content.documentos.length > 0;

  return (
    <div className="space-y-6" data-testid="folder-list">
      {/* Subcarpetas */}
      {hasFolders && (
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">
            Carpetas ({content.total_subcarpetas})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {content.subcarpetas.map((folder) => (
              <FolderItem
                key={folder.id}
                item={folder}
                onClick={() => onFolderClick(folder.id)}
                onDeleteClick={onDeleteClick}
              />
            ))}
          </div>
        </section>
      )}

      {/* Documentos */}
      {hasDocuments && (
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">
            Documentos ({content.total_documentos})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {content.documentos.map((document) => (
              <FolderItem
                key={document.id}
                item={document}
                onClick={() => {
                  // TODO: Navegación a detalle de documento (Post-MVP)
                  console.log('Ver documento:', document.id);
                }}
                onDeleteClick={onDeleteClick}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  );
};
