/**
 * Notification type for visual styling
 */
export type NotificationType = 'success' | 'error' | 'warning' | 'info';

/**
 * Notification object structure
 */
export interface Notification {
  /** Unique identifier for the notification */
  id: string;
  /** Notification message to display */
  message: string;
  /** Visual type of the notification */
  type: NotificationType;
  /** Timestamp when the notification was created */
  createdAt: number;
}
