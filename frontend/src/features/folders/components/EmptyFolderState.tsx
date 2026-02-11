/**
 * EmptyFolderState - Estado vacío cuando carpeta no tiene contenido
 * Muestra mensaje descriptivo y botón de acción si usuario tiene permisos
 */
import React from 'react';
import { Button } from '@ui/forms/Button';

interface EmptyFolderStateProps {
  canWrite: boolean;
  onCreateClick: () => void;
}

export const EmptyFolderState: React.FC<EmptyFolderStateProps> = ({
  canWrite,
  onCreateClick,
}) => {
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
      {canWrite && (
        <Button
          onClick={onCreateClick}
          variant="primary"
          fullWidth={false}
        >
          + Nueva carpeta
        </Button>
      )}
    </div>
  );
};
