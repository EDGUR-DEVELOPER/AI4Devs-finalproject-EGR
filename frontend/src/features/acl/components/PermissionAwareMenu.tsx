/**
 * Permission-aware context menu for actions
 */

import React, { useMemo, useState } from 'react';
import { canPerformAction, getDisabledActionMessage } from '../utils/permissionEvaluator';
import { PermissionTooltip } from './PermissionTooltip';
import type { ICapabilities } from '../types';

export interface MenuAction {
  id: string;
  label: string;
  icon?: React.ReactNode;
  onClick: () => void | Promise<void>;
  variant?: 'default' | 'danger';
}

export interface PermissionAwareMenuProps {
  actions: MenuAction[];
  capabilities: ICapabilities;
  trigger?: React.ReactNode;
  showDisabledItems?: boolean;
}

export const PermissionAwareMenu: React.FC<PermissionAwareMenuProps> = ({
  actions,
  capabilities,
  trigger,
  showDisabledItems = true,
}) => {
  const [open, setOpen] = useState(false);

  const preparedActions = useMemo(() => {
    return actions.map((action) => {
      const allowed = canPerformAction(capabilities, action.id);
      const disabledMessage = getDisabledActionMessage(action.id, null) ?? '';

      return {
        ...action,
        allowed,
        disabledMessage,
      };
    });
  }, [actions, capabilities]);

  const visibleActions = showDisabledItems
    ? preparedActions
    : preparedActions.filter((action) => action.allowed);

  if (visibleActions.length === 0) {
    return null;
  }

  return (
    <div className="relative inline-flex">
      <button
        type="button"
        onClick={(event) => {
          event.stopPropagation();
          setOpen((prev) => !prev);
        }}
        className="inline-flex items-center justify-center rounded-md px-2 py-1 text-sm text-secondary-700 hover:bg-secondary-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
        aria-haspopup="true"
        aria-expanded={open}
      >
        {trigger ?? '⋮'}
      </button>

      {open && (
        <div
          className="absolute right-0 mt-2 w-56 rounded-md border border-secondary-200 bg-white shadow-lg z-20"
          role="menu"
        >
          {visibleActions.map((action) => {
            const isDisabled = !action.allowed;
            const itemClasses = [
              'flex w-full items-center gap-2 px-4 py-2 text-left text-sm',
              isDisabled ? 'cursor-not-allowed text-secondary-400' : 'hover:bg-secondary-50',
              action.variant === 'danger' && !isDisabled ? 'text-red-600' : 'text-secondary-800',
            ]
              .filter(Boolean)
              .join(' ');

            const item = (
              <button
                key={action.id}
                type="button"
                role="menuitem"
                disabled={isDisabled}
                aria-disabled={isDisabled}
                className={itemClasses}
                onClick={async (event) => {
                  event.stopPropagation();
                  if (isDisabled) {
                    return;
                  }
                  await action.onClick();
                  setOpen(false);
                }}
              >
                {action.icon && <span aria-hidden="true">{action.icon}</span>}
                <span>{action.label}</span>
              </button>
            );

            if (!isDisabled) {
              return item;
            }

            return (
              <PermissionTooltip
                key={action.id}
                message={action.disabledMessage || 'No tienes permiso para esta accion'}
                disabled={isDisabled}
              >
                {item}
              </PermissionTooltip>
            );
          })}
        </div>
      )}
    </div>
  );
};
