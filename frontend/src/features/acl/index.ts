/**
 * ACL Feature Barrel Export
 * Central export point for all ACL-related types, hooks, and components
 */

// Types and Domain Models
export type { INivelAcceso, CodigoNivelAcceso, AccionPermitida } from './types';

// Custom Hooks
export { useNivelesAcceso } from './hooks/useNivelesAcceso';
export type { UseNivelesAccesoReturn } from './hooks/useNivelesAcceso';

// Components
export { NivelAccesoSelect } from './components/NivelAccesoSelect';
export type { NivelAccesoSelectProps } from './components/NivelAccesoSelect';

// API Services
export { aclApi } from './services/nivelAccesoService';
