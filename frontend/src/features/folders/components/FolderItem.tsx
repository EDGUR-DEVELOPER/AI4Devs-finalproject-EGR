/**
 * FolderItem - Tarjeta individual de carpeta o documento
 * Diferenciación visual por tipo, con acciones contextuales
 */
import React, { useState } from 'react';
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
  const [menuOpen, setMenuOpen] = useState(false);
  const isFolder = item.tipo === 'carpeta';
  
  // Determinar si usuario puede eliminar
  const canDelete = 'puede_administrar' in item && item.puede_administrar;

  return (
    <div
      className="relative bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer group"
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onClick();
        }
      }}
      role="button"
      tabIndex={0}
      aria-label={`${isFolder ? 'Carpeta' : 'Documento'}: ${item.nombre}`}
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

        {/* Menú contextual (solo carpetas con permiso admin) */}
        {isFolder && canDelete && (
          <div className="relative">
            <button
              onClick={(e) => {
                e.stopPropagation();
                setMenuOpen(!menuOpen);
              }}
              className="p-1 text-gray-400 hover:text-gray-600 rounded opacity-0 group-hover:opacity-100 transition-opacity focus:opacity-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              aria-label="Opciones"
              aria-haspopup="true"
              aria-expanded={menuOpen}
            >
              ⋮
            </button>

            {menuOpen && (
              <div 
                className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg border border-gray-200 z-10"
                role="menu"
              >
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDeleteClick(item.id);
                    setMenuOpen(false);
                  }}
                  className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-md"
                  role="menuitem"
                >
                  Eliminar carpeta
                </button>
              </div>
            )}
          </div>
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
