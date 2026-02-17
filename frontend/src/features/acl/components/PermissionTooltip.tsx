/**
 * Simple tooltip wrapper for permission feedback
 */

import React from 'react';

export interface PermissionTooltipProps {
  message: string;
  children: React.ReactNode;
  disabled?: boolean;
}

export const PermissionTooltip: React.FC<PermissionTooltipProps> = ({
  message,
  children,
  disabled = false,
}) => {
  if (!disabled || !message) {
    return <>{children}</>;
  }

  return (
    <span
      className="relative inline-flex group"
      tabIndex={0}
      aria-label={message}
    >
      {children}
      <span
        className="pointer-events-none absolute left-1/2 top-full z-10 mt-2 -translate-x-1/2 whitespace-nowrap rounded-md bg-secondary-900 px-3 py-2 text-xs text-white opacity-0 shadow-lg transition-opacity duration-150 group-hover:opacity-100 group-focus-within:opacity-100"
        role="tooltip"
      >
        {message}
      </span>
    </span>
  );
};
