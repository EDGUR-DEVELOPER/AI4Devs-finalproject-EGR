/**
 * Permission evaluator utilities
 * Pure functions without React dependencies
 */

import type { CodigoNivelAcceso, ICapabilities } from '../types';
import {
  ACTION_REQUIREMENTS,
  DISABLED_ACTION_MESSAGES,
  PERMISSION_TO_CAPABILITIES,
} from '@common/constants/permissions';

const ACTION_TO_CAPABILITY: Record<string, keyof ICapabilities> = {
  ver: 'canRead',
  listar: 'canRead',
  descargar: 'canDownload',
  subir: 'canUpload',
  modificar: 'canWrite',
  crear_version: 'canCreateVersion',
  cambiar_version_actual: 'canChangeVersion',
  eliminar_carpeta: 'canDeleteFolder',
  administrar_permisos: 'canManagePermissions',
  crear_carpeta: 'canWrite',
};

/**
 * Convert access level to user capabilities
 */
export function getCapabilitiesFromLevel(
  nivelAcceso: CodigoNivelAcceso | null
): ICapabilities {
  if (!nivelAcceso) {
    return PERMISSION_TO_CAPABILITIES.NINGUNO;
  }

  return PERMISSION_TO_CAPABILITIES[nivelAcceso] ?? PERMISSION_TO_CAPABILITIES.NINGUNO;
}

/**
 * Evaluate if a specific action is allowed
 */
export function canPerformAction(
  capabilities: ICapabilities,
  action: string
): boolean {
  const capabilityKey = ACTION_TO_CAPABILITY[action];

  if (!capabilityKey) {
    return false;
  }

  return Boolean(capabilities[capabilityKey]);
}

/**
 * Get descriptive message for a disabled action
 */
export function getDisabledActionMessage(
  action: string,
  currentLevel: CodigoNivelAcceso | null
): string | null {
  const capabilities = getCapabilitiesFromLevel(currentLevel);

  if (canPerformAction(capabilities, action)) {
    return null;
  }

  if (action in DISABLED_ACTION_MESSAGES) {
    return DISABLED_ACTION_MESSAGES[action];
  }

  if (ACTION_REQUIREMENTS[action] === null) {
    return DISABLED_ACTION_MESSAGES.no_access;
  }

  return DISABLED_ACTION_MESSAGES.no_access;
}

/**
 * Filter available actions based on user capabilities
 */
export function filterAvailableActions(
  allActions: string[],
  capabilities: ICapabilities
): string[] {
  return allActions.filter((action) => canPerformAction(capabilities, action));
}
