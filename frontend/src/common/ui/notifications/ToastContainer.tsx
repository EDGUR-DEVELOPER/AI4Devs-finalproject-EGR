import { useNotificationStore } from './useNotificationStore';
import { Toast } from './Toast';

/**
 * ToastContainer component - renders all active notifications
 * Should be mounted once at the root level of the application
 */
export function ToastContainer() {
  const notifications = useNotificationStore((state) => state.notifications);

  if (notifications.length === 0) {
    return null;
  }

  return (
    <div
      className="fixed top-4 right-4 z-50 flex flex-col items-end"
      aria-live="polite"
      aria-atomic="true"
    >
      {notifications.map((notification) => (
        <Toast key={notification.id} notification={notification} />
      ))}
    </div>
  );
}
