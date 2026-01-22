import React from 'react';
import { DASHBOARD_MESSAGES } from '../constants/modules';

interface WelcomeCardProps {
    /** Nombre del usuario a saludar */
    userName: string;
    /** Roles del usuario */
    roles: string[];
}

/**
 * Tarjeta de bienvenida del dashboard
 * Muestra saludo personalizado y roles del usuario
 * Mobile-first: diseño compacto que se expande en desktop
 */
export const WelcomeCard: React.FC<WelcomeCardProps> = ({ userName, roles }) => {
    // Obtener hora del día para saludo contextual
    const getGreeting = () => {
        const hour = new Date().getHours();
        if (hour < 12) return 'Buenos días';
        if (hour < 19) return 'Buenas tardes';
        return 'Buenas noches';
    };

    return (
        <div className="bg-gradient-to-br from-primary-600 to-primary-800 rounded-2xl p-6 text-white shadow-lg">
            {/* Saludo principal */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <p className="text-primary-100 text-sm font-medium">
                        {getGreeting()}
                    </p>
                    <h1 className="text-2xl sm:text-3xl font-bold mt-1">
                        {DASHBOARD_MESSAGES.WELCOME(userName)}
                    </h1>
                    <p className="text-primary-200 mt-2 text-sm sm:text-base">
                        {DASHBOARD_MESSAGES.WELCOME_BACK}
                    </p>
                </div>

                {/* Indicador de rol */}
                <div className="flex flex-wrap gap-2">
                    {roles.map((role) => (
                        <span
                            key={role}
                            className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-white/20 text-white backdrop-blur-sm"
                        >
                            {role}
                        </span>
                    ))}
                </div>
            </div>

            {/* Decoración visual */}
            <div className="absolute top-0 right-0 -mt-4 -mr-4 w-24 h-24 bg-white/10 rounded-full blur-2xl pointer-events-none" />
        </div>
    );
};
