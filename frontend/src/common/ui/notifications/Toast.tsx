import type { Notification } from '@core/domain/notifications/types';
import { useNotificationStore } from './useNotificationStore';

interface ToastProps {
  notification: Notification;
}

/**
 * Toast component - individual notification display
 * Uses Tailwind CSS for styling with animations
 */
export function Toast({ notification }: ToastProps) {
  const dismissNotification = useNotificationStore(
    (state) => state.dismissNotification
  );

  const typeStyles = {
    success: 'bg-green-500 text-white',
    error: 'bg-red-500 text-white',
    warning: 'bg-yellow-500 text-white',
    info: 'bg-blue-500 text-white',
  };

  const typeIcons = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ',
  };

  return (
    <div
      className={`${typeStyles[notification.type]} rounded-lg shadow-lg p-4 mb-3 min-w-80 max-w-md animate-slide-in flex items-start gap-3`}
      role="alert"
    >
      <span className="text-xl font-bold shrink-0">
        {typeIcons[notification.type]}
      </span>
      <p className="flex-1 text-sm">{notification.message}</p>
      <button
        onClick={() => dismissNotification(notification.id)}
        className="shrink-0 text-white hover:opacity-70 transition-opacity ml-2"
        aria-label="Cerrar notificación"
      >
        ✕
      </button>
    </div>
  );
}
