/**
 * Página 404 - Recurso no encontrado
 * 
 * Componente genérico para manejar errores 404 con security by obscurity:
 * No diferencia entre "recurso inexistente" y "sin acceso por aislamiento multi-tenant"
 * 
 * Casos de uso:
 * 1. Página/ruta no existe en el router
 * 2. Recurso específico no encontrado (404 de API)
 * 3. Intento de acceso a recurso de otra organización (traducido a 404)
 */

import { useNavigate } from 'react-router-dom';
import { Button } from '@ui/forms/Button';

interface NotFoundPageProps {
  /**
   * Mensaje personalizado opcional
   * Por defecto muestra mensaje genérico de seguridad
   */
  message?: string;
}

/**
 * NotFoundPage - Página de error 404 con diseño responsive
 * 
 * @example
 * // Uso básico en router
 * <Route path="*" element={<NotFoundPage />} />
 * 
 * // Con mensaje personalizado
 * <Route path="/404" element={<NotFoundPage message="El documento no está disponible" />} />
 */
export function NotFoundPage({ message }: NotFoundPageProps) {
  const navigate = useNavigate();

  const defaultMessage = 'Recurso no encontrado o sin acceso';

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full text-center space-y-8">
        {/* Icono SVG de documento bloqueado */}
        <div className="flex justify-center">
          <svg
            className="w-24 h-24 text-gray-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </div>

        {/* Código de error */}
        <div>
          <h1 className="text-6xl font-bold text-gray-900 mb-2">404</h1>
          <p className="text-xl font-semibold text-gray-700 mb-4">
            {message || defaultMessage}
          </p>
          <p className="text-sm text-gray-500 max-w-sm mx-auto">
            El recurso que buscas no existe, fue eliminado, o no tienes los
            permisos necesarios para acceder a él.
          </p>
        </div>

        {/* Acciones */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Button
            onClick={() => navigate('/')}
            variant="primary"
            fullWidth={false}
          >
            Volver al inicio
          </Button>
          <Button
            onClick={() => navigate(-1)}
            variant="secondary"
            fullWidth={false}
          >
            Página anterior
          </Button>
        </div>

        {/* Información adicional */}
        <div className="mt-8 pt-8 border-t border-gray-200">
          <p className="text-xs text-gray-400">
            Si crees que esto es un error, contacta con el administrador de tu
            organización.
          </p>
        </div>
      </div>
    </div>
  );
}
