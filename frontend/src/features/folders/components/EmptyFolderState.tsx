/**
 * EmptyFolderState - Estado vacío cuando carpeta no tiene contenido
 * Muestra mensaje descriptivo y botón de acción si usuario tiene permisos
 */
import React from 'react';
import { PermissionAwareButton } from '@features/acl';
import type { ICapabilities } from '@features/acl';

interface EmptyFolderStateProps {
  capabilities: ICapabilities;
  onCreateClick: () => void;
}

export const EmptyFolderState: React.FC<EmptyFolderStateProps> = ({
  capabilities,
  onCreateClick,
}) => {
  const canWrite = capabilities.canWrite;

  return (
    <div 
      className="flex flex-col items-center justify-center py-16 px-4 text-center"
      role="status"
      aria-live="polite"
    >
      {/* Icono ilustrativo */}
      <div className="text-6xl mb-4" aria-hidden="true">
        📂
      </div>

      {/* Mensaje */}
      <h3 className="text-lg font-medium text-gray-900 mb-2">
        Esta carpeta está vacía
      </h3>
      
      <p className="text-sm text-gray-500 mb-6 max-w-md">
        {canWrite 
          ? 'Comienza creando una nueva carpeta para organizar tus documentos.'
          : 'No hay contenido disponible en esta carpeta.'}
      </p>

      {/* CTA solo si tiene permisos */}
      <PermissionAwareButton
        onClick={onCreateClick}
        action="crear_carpeta"
        capabilities={capabilities}
        variant="primary"
        fullWidth={false}
      >
        + Nueva carpeta
      </PermissionAwareButton>
    </div>
  );
};
