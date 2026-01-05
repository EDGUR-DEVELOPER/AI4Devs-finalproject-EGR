import { create } from 'zustand';
import type { Notification, NotificationType } from '@core/domain/notifications/types';

interface NotificationState {
  /** Array of active notifications */
  notifications: Notification[];
  /** Add a new notification */
  showNotification: (message: string, type: NotificationType) => void;
  /** Remove a notification by ID */
  dismissNotification: (id: string) => void;
}

/**
 * Global notification store
 * Manages toast notifications throughout the application
 */
export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],

  showNotification: (message: string, type: NotificationType) => {
    const id = `notification-${Date.now()}-${Math.random()}`;
    const notification: Notification = {
      id,
      message,
      type,
      createdAt: Date.now(),
    };

    set((state) => ({
      notifications: [...state.notifications, notification],
    }));

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
      set((state) => ({
        notifications: state.notifications.filter((n) => n.id !== id),
      }));
    }, 5000);
  },

  dismissNotification: (id: string) => {
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    }));
  },
}));
