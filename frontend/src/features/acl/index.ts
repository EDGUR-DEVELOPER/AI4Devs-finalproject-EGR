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
  IAclDocumento,
  CreateAclDocumentoDTO,
  UpdateAclDocumentoDTO,
  AclDocumentoApiResponse,
  ListAclDocumentoApiResponse,
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

export { useAclDocumento } from './hooks/useAclDocumento';
export type { UseAclDocumentoReturn } from './hooks/useAclDocumento';

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

export { AclDocumentoModal } from './components/AclDocumentoModal';
export type { AclDocumentoModalProps } from './components/AclDocumentoModal';

export { AclDocumentoList } from './components/AclDocumentoList';
export type { AclDocumentoListProps } from './components/AclDocumentoList';

export { AclDocumentoSection } from './components/AclDocumentoSection';
export type { AclDocumentoSectionProps } from './components/AclDocumentoSection';

// ============================================================================
// API SERVICES
// ============================================================================

export { aclApi } from './services/nivelAccesoService';
export { aclCarpetaApi } from './services/aclCarpetaService';
export { aclDocumentoApi } from './services/aclDocumentoService';
