import React from 'react';
import { Link } from 'react-router-dom';

/**
 * Página mostrada cuando el usuario no tiene permisos de administrador
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const AccessDeniedPage: React.FC = () => {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <div className="text-center px-4">
                {/* Icono de error */}
                <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-red-100 mb-6">
                    <svg className="w-10 h-10 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                </div>

                {/* Código de error */}
                <h1 className="text-6xl font-bold text-red-500 mb-4">403</h1>

                {/* Título */}
                <h2 className="text-2xl font-semibold text-gray-800 mb-2">
                    Acceso Denegado
                </h2>

                {/* Descripción */}
                <p className="text-gray-600 mb-8 max-w-md mx-auto">
                    No tienes permisos para acceder a esta sección.
                    Contacta a un administrador si crees que esto es un error.
                </p>

                {/* Botón de regreso */}
                <Link
                    to="/"
                    className="inline-flex items-center px-6 py-3 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
                >
                    <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                    </svg>
                    Volver al inicio
                </Link>
            </div>
        </div>
    );
};
