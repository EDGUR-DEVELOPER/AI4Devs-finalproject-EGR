import React from 'react';
import { useDashboard } from '../hooks/useDashboard';
import {
    DashboardLayout,
    WelcomeCard,
    ModulesGrid,
    SystemInfoCard,
    QuickActions,
} from '../components';

/**
 * Página principal del Dashboard
 * Muestra información del sistema y acceso a módulos
 * Mobile-first: layout de una columna en móvil, dos en desktop
 */
export const DashboardPage: React.FC = () => {
    const {
        userName,
        availableModules,
        quickActions,
        systemInfo,
        userRoles,
    } = useDashboard();

    return (
        <DashboardLayout>
            <div className="space-y-6">
                {/* Tarjeta de bienvenida - ancho completo */}
                <WelcomeCard userName={userName} roles={userRoles} />

                {/* Grid de dos columnas en desktop */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Columna principal - módulos y acciones */}
                    <div className="lg:col-span-2 space-y-6">
                        {/* Acciones rápidas */}
                        <QuickActions actions={quickActions} />

                        {/* Grid de módulos */}
                        <ModulesGrid modules={availableModules} />
                    </div>

                    {/* Columna lateral - información del sistema */}
                    <div className="space-y-6">
                        <SystemInfoCard info={systemInfo} />

                        {/* Tip del día / Ayuda rápida */}
                        <div className="bg-linear-to-br from-indigo-50 to-purple-50 rounded-xl border border-indigo-100 p-4 sm:p-6">
                            <div className="flex items-start gap-3">
                                <div className="shrink-0">
                                    <svg className="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                </div>
                                <div>
                                    <h3 className="font-semibold text-indigo-900 text-sm">
                                        ¿Sabías que...?
                                    </h3>
                                    <p className="text-indigo-700 text-sm mt-1">
                                        Puedes acceder rápidamente a cualquier módulo desde el menú superior.
                                        Usa los atajos de teclado para navegar más rápido.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
};
