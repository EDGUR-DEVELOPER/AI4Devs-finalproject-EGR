/**
 * Breadcrumb component - Navegación jerárquica de carpetas
 * Muestra ruta clickeable para navegar a carpetas padre
 */
import React from 'react';
import type { BreadcrumbSegment } from '../types/folder.types';

interface BreadcrumbProps {
  segments: BreadcrumbSegment[];
  onNavigate: (folderId: string | undefined) => void;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ segments, onNavigate }) => {
  if (segments.length === 0) {
    return null;
  }

  return (
    <nav 
      className="flex items-center space-x-2 text-sm text-gray-600 mb-4 py-2" 
      aria-label="Breadcrumb"
    >
      {segments.map((segment, index) => {
        const isLast = index === segments.length - 1;
        const segmentKey = `${segment.id ?? 'root'}-${index}`;
        
        return (
          <React.Fragment key={segmentKey}>
            {index > 0 && (
              <span className="text-gray-400" aria-hidden="true">
                /
              </span>
            )}
            
            {isLast ? (
              <span 
                className="font-semibold text-gray-900"
                aria-current="page"
              >
                {segment.nombre}
              </span>
            ) : (
              <button
                onClick={() => onNavigate(segment.id)}
                className="hover:underline hover:text-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded px-1"
                data-testid={`breadcrumb-segment-${segment.id}`}
              >
                {segment.nombre}
              </button>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
};
