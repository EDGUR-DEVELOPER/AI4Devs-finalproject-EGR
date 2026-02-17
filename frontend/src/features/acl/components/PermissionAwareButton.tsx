/**
 * Button that disables itself based on permission capabilities
 */

import React, { useMemo } from 'react';
import { Button, type ButtonProps } from '@ui/forms/Button';
import { canPerformAction, getDisabledActionMessage } from '../utils/permissionEvaluator';
import { PermissionTooltip } from './PermissionTooltip';
import type { ICapabilities } from '../types';

export interface PermissionAwareButtonProps extends ButtonProps {
  children: React.ReactNode;
  action: string;
  capabilities: ICapabilities;
  disabledMessage?: string;
  showTooltipOnDisabled?: boolean;
}

export const PermissionAwareButton: React.FC<PermissionAwareButtonProps> = ({
  children,
  action,
  capabilities,
  disabledMessage,
  showTooltipOnDisabled = true,
  onClick,
  ...props
}) => {
  const isAllowed = useMemo(
    () => canPerformAction(capabilities, action),
    [capabilities, action]
  );

  const disabledReason = useMemo(() => {
    if (isAllowed) {
      return '';
    }

    return disabledMessage ?? getDisabledActionMessage(action, null) ?? '';
  }, [action, disabledMessage, isAllowed]);

  const isDisabled = !isAllowed || props.disabled;

  const buttonElement = (
    <Button
      {...props}
      disabled={isDisabled}
      onClick={isAllowed ? onClick : undefined}
      aria-disabled={isDisabled}
      title={showTooltipOnDisabled && isDisabled ? disabledReason : undefined}
    >
      {children}
    </Button>
  );

  if (!showTooltipOnDisabled || !isDisabled) {
    return buttonElement;
  }

  return (
    <PermissionTooltip message={disabledReason} disabled={isDisabled}>
      {buttonElement}
    </PermissionTooltip>
  );
};
