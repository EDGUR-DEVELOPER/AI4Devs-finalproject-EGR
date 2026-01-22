import React from 'react';
import type { SystemInfo } from '../types/dashboard.types';
import { DASHBOARD_MESSAGES } from '../constants/modules';

interface SystemInfoCardProps {
  /** Información del sistema */
  info: SystemInfo;
}

/** Mapeo de entornos a colores */
const environmentColors = {
  development: 'bg-yellow-100 text-yellow-800',
  staging: 'bg-blue-100 text-blue-800',
  production: 'bg-green-100 text-green-800',
};

/** Mapeo de entornos a etiquetas */
const environmentLabels = {
  development: 'Desarrollo',
  staging: 'Staging',
  production: 'Producción',
};

/**
 * Tarjeta de información del sistema
 * Muestra versión, entorno y última sesión
 */
export const SystemInfoCard: React.FC<SystemInfoCardProps> = ({ info }) => {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-4 sm:p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">
        {DASHBOARD_MESSAGES.SYSTEM_INFO}
      </h2>

      <div className="space-y-3">
        {/* Nombre de la aplicación */}
        <div className="flex items-center justify-between">
          <span className="text-gray-500 text-sm">Aplicación</span>
          <span className="font-medium text-gray-900">{info.appName}</span>
        </div>

        {/* Versión */}
        <div className="flex items-center justify-between">
          <span className="text-gray-500 text-sm">{DASHBOARD_MESSAGES.VERSION}</span>
          <span className="font-mono text-sm text-gray-700 bg-gray-100 px-2 py-0.5 rounded">
            v{info.version}
          </span>
        </div>

        {/* Entorno */}
        <div className="flex items-center justify-between">
          <span className="text-gray-500 text-sm">{DASHBOARD_MESSAGES.ENVIRONMENT}</span>
          <span className={`text-xs font-medium px-2 py-1 rounded-full ${environmentColors[info.environment]}`}>
            {environmentLabels[info.environment]}
          </span>
        </div>

        {/* Última sesión */}
        {info.lastLogin && (
          <div className="flex items-center justify-between pt-2 border-t border-gray-100">
            <span className="text-gray-500 text-sm">{DASHBOARD_MESSAGES.LAST_LOGIN}</span>
            <span className="text-gray-700 text-sm">{info.lastLogin}</span>
          </div>
        )}
      </div>
    </div>
  );
};
