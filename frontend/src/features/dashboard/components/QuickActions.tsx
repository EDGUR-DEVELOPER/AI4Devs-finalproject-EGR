import React from 'react';
import { Link } from 'react-router-dom';
import type { QuickAction } from '../types/dashboard.types';
import { DashboardIcon } from './DashboardIcon';
import { DASHBOARD_MESSAGES } from '../constants/modules';

interface QuickActionsProps {
    /** Lista de acciones rápidas */
    actions: QuickAction[];
}

/**
 * Panel de acciones rápidas
 * Botones de acceso directo a funciones frecuentes
 */
export const QuickActions: React.FC<QuickActionsProps> = ({ actions }) => {
    if (actions.length === 0) return null;

    return (
        <div className="bg-white rounded-xl border border-gray-200 p-4 sm:p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
                {DASHBOARD_MESSAGES.QUICK_ACCESS}
            </h2>

            <div className="flex flex-col sm:flex-row gap-3">
                {actions.map((action) => (
                    <Link
                        key={action.id}
                        to={action.path}
                        className={`
              flex items-center justify-center gap-2 px-4 py-3 rounded-lg font-medium 
              transition-all duration-200 text-sm sm:text-base
              ${action.variant === 'primary'
                                ? 'bg-primary-600 text-white hover:bg-primary-700 shadow-sm hover:shadow-md'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            }
            `}
                    >
                        <DashboardIcon name={action.icon} className="w-5 h-5" />
                        {action.label}
                    </Link>
                ))}
            </div>
        </div>
    );
};
