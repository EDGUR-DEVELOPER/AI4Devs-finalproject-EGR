import React from 'react';
import { DashboardHeader } from './DashboardHeader';

interface DashboardLayoutProps {
    /** Contenido principal del dashboard */
    children: React.ReactNode;
}

/**
 * Layout principal del dashboard
 * Incluye header con navegación y área de contenido
 * Mobile-first: full-width en móvil, con márgenes en desktop
 */
export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ children }) => {
    return (
        <div className="min-h-screen bg-gray-50">
            {/* Header fijo */}
            <DashboardHeader />

            {/* Contenido principal */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
                {children}
            </main>

            {/* Footer simple */}
            <footer className="border-t border-gray-200 bg-white mt-auto">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
                    <p className="text-center text-xs text-gray-500">
                        © {new Date().getFullYear()} Sistema de Gestión Documental. Todos los derechos reservados.
                    </p>
                </div>
            </footer>
        </div>
    );
};
