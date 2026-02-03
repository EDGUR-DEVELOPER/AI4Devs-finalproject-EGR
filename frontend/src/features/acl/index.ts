/**
 * ACL Feature Barrel Export
 * Central export point for all ACL-related types, hooks, and components
 */

// ============================================================================
// TYPES AND DOMAIN MODELS
// ============================================================================

export type {
  INivelAcceso,
  CodigoNivelAcceso,
  AccionPermitida,
  IUsuario,
  IAclCarpeta,
  IPermisoEfectivo,
  AclCarpetaListItem,
  CreateAclCarpetaDTO,
  UpdateAclCarpetaDTO,
  AclCarpetaApiResponse,
  ListAclCarpetaApiResponse,
  AclErrorResponse,
} from './types';

// ============================================================================
// CUSTOM HOOKS
// ============================================================================

export { useNivelesAcceso } from './hooks/useNivelesAcceso';
export type { UseNivelesAccesoReturn } from './hooks/useNivelesAcceso';

export { useAclCarpeta } from './hooks/useAclCarpeta';
export type { UseAclCarpetaReturn } from './hooks/useAclCarpeta';

export { useMiPermisoCarpeta } from './hooks/useMiPermisoCarpeta';
export type { UseMiPermisoCarpetaReturn } from './hooks/useMiPermisoCarpeta';

// ============================================================================
// ATOMIC COMPONENTS
// ============================================================================

export { PermissionBadge } from './components/PermissionBadge';
export type { PermissionBadgeProps } from './components/PermissionBadge';

export { RecursiveIndicator } from './components/RecursiveIndicator';
export type { RecursiveIndicatorProps } from './components/RecursiveIndicator';

// ============================================================================
// MOLECULE COMPONENTS
// ============================================================================

export { UserSelect } from './components/UserSelect';
export type { UserSelectProps } from './components/UserSelect';

// ============================================================================
// ORGANISM COMPONENTS
// ============================================================================

export { AclCarpetaModal } from './components/AclCarpetaModal';
export type { AclCarpetaModalProps } from './components/AclCarpetaModal';

export { AclCarpetaList } from './components/AclCarpetaList';
export type { AclCarpetaListProps } from './components/AclCarpetaList';

// ============================================================================
// FEATURE INTEGRATION COMPONENT
// ============================================================================

export { AclCarpetaSection } from './components/AclCarpetaSection';
export type { AclCarpetaSectionProps } from './components/AclCarpetaSection';

// ============================================================================
// API SERVICES
// ============================================================================

export { aclApi } from './services/nivelAccesoService';
export { aclCarpetaApi } from './services/aclCarpetaService';
