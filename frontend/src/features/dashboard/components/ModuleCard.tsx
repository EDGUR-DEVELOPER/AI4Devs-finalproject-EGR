import React from 'react';
import { Link } from 'react-router-dom';
import type { SystemModule, ModuleColor } from '../types/dashboard.types';
import { DashboardIcon } from './DashboardIcon';
import { DASHBOARD_MESSAGES } from '../constants/modules';

interface ModuleCardProps {
    /** Información del módulo */
    module: SystemModule;
}

/** Mapeo de colores a clases de Tailwind */
const colorClasses: Record<ModuleColor, { bg: string; icon: string; hover: string }> = {
    blue: {
        bg: 'bg-blue-50',
        icon: 'text-blue-600',
        hover: 'hover:bg-blue-100 hover:border-blue-200',
    },
    green: {
        bg: 'bg-green-50',
        icon: 'text-green-600',
        hover: 'hover:bg-green-100 hover:border-green-200',
    },
    purple: {
        bg: 'bg-purple-50',
        icon: 'text-purple-600',
        hover: 'hover:bg-purple-100 hover:border-purple-200',
    },
    orange: {
        bg: 'bg-orange-50',
        icon: 'text-orange-600',
        hover: 'hover:bg-orange-100 hover:border-orange-200',
    },
    red: {
        bg: 'bg-red-50',
        icon: 'text-red-600',
        hover: 'hover:bg-red-100 hover:border-red-200',
    },
    teal: {
        bg: 'bg-teal-50',
        icon: 'text-teal-600',
        hover: 'hover:bg-teal-100 hover:border-teal-200',
    },
    indigo: {
        bg: 'bg-indigo-50',
        icon: 'text-indigo-600',
        hover: 'hover:bg-indigo-100 hover:border-indigo-200',
    },
    pink: {
        bg: 'bg-pink-50',
        icon: 'text-pink-600',
        hover: 'hover:bg-pink-100 hover:border-pink-200',
    },
};

/**
 * Tarjeta de módulo del sistema
 * Mobile-first: layout vertical en móvil, se adapta en desktop
 */
export const ModuleCard: React.FC<ModuleCardProps> = ({ module }) => {
    const colors = colorClasses[module.color];
    const isDisabled = !module.enabled;

    // Contenido de la tarjeta
    const cardContent = (
        <>
            {/* Icono con fondo de color */}
            <div className={`w-12 h-12 sm:w-14 sm:h-14 rounded-xl ${colors.bg} flex items-center justify-center flex-shrink-0`}>
                <DashboardIcon name={module.icon} className={`w-6 h-6 sm:w-7 sm:h-7 ${colors.icon}`} />
            </div>

            {/* Información del módulo */}
            <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-gray-900 text-sm sm:text-base truncate">
                        {module.name}
                    </h3>
                    {isDisabled && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-500">
                            {DASHBOARD_MESSAGES.COMING_SOON}
                        </span>
                    )}
                </div>
                <p className="text-gray-500 text-xs sm:text-sm mt-1 line-clamp-2">
                    {module.description}
                </p>
            </div>

            {/* Flecha indicadora (solo si está habilitado) */}
            {!isDisabled && (
                <div className="flex-shrink-0 text-gray-400 group-hover:text-gray-600 transition-colors">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                </div>
            )}
        </>
    );

    // Si está deshabilitado, renderizar como div
    if (isDisabled) {
        return (
            <div
                className="group flex items-center gap-4 p-4 bg-white rounded-xl border border-gray-200 opacity-60 cursor-not-allowed"
            >
                {cardContent}
            </div>
        );
    }

    // Si está habilitado, renderizar como Link
    return (
        <Link
            to={module.path}
            className={`group flex items-center gap-4 p-4 bg-white rounded-xl border border-gray-200 transition-all duration-200 ${colors.hover} shadow-sm hover:shadow-md`}
        >
            {cardContent}
        </Link>
    );
};
