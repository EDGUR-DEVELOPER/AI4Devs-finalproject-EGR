import React from 'react';

/**
 * Skeleton de carga para la tabla de usuarios
 * Muestra animación placeholder mientras carga datos
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const UsersTableSkeleton: React.FC = () => {
    return (
        <div className="animate-pulse">
            {/* Header skeleton */}
            <div className="flex items-center space-x-4 py-3 px-6 bg-gray-50 border-b border-gray-200">
                <div className="h-4 bg-gray-200 rounded w-1/4" />
                <div className="h-4 bg-gray-200 rounded w-1/5" />
                <div className="h-4 bg-gray-200 rounded w-16" />
                <div className="h-4 bg-gray-200 rounded w-1/4" />
                <div className="h-4 bg-gray-200 rounded w-20" />
            </div>

            {/* Filas skeleton */}
            {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="flex items-center space-x-4 py-4 px-6 border-b border-gray-200">
                    <div className="h-4 bg-gray-200 rounded w-1/4" />
                    <div className="h-4 bg-gray-200 rounded w-1/5" />
                    <div className="h-6 bg-gray-200 rounded-full w-16" />
                    <div className="flex gap-1 w-1/4">
                        <div className="h-6 bg-gray-200 rounded w-16" />
                        <div className="h-6 bg-gray-200 rounded w-12" />
                    </div>
                    <div className="h-4 bg-gray-200 rounded w-20" />
                </div>
            ))}
        </div>
    );
};
