import React from 'react';
import type { SystemModule } from '../types/dashboard.types';
import { ModuleCard } from './ModuleCard';
import { DASHBOARD_MESSAGES } from '../constants/modules';

interface ModulesGridProps {
    /** Lista de módulos a mostrar */
    modules: SystemModule[];
}

/**
 * Grid de módulos del sistema
 * Mobile-first: 1 columna en móvil, 2 en tablet, 3 en desktop
 */
export const ModulesGrid: React.FC<ModulesGridProps> = ({ modules }) => {
    if (modules.length === 0) {
        return (
            <div className="text-center py-12">
                <p className="text-gray-500">{DASHBOARD_MESSAGES.NO_MODULES_AVAILABLE}</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {/* Título de sección */}
            <h2 className="text-lg font-semibold text-gray-900">
                {DASHBOARD_MESSAGES.MODULES}
            </h2>

            {/* Grid responsivo */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {modules.map((module) => (
                    <ModuleCard key={module.id} module={module} />
                ))}
            </div>
        </div>
    );
};
