/**
 * Barrel export - API pública del feature folders
 * Exporta solo lo necesario para uso externo
 */

// Componentes principales
export { FolderExplorer } from './components/FolderExplorer';
export { FoldersPage } from './pages/FoldersPage';

// Types (para uso en otras features)
export type {
  FolderItem,
  DocumentItem,
  FolderContent,
  FolderPermissions,
  BreadcrumbSegment,
} from './types/folder.types';

// Hooks (opcional - para uso avanzado)
export { useFolderContent } from './hooks/useFolderContent';
export { useBreadcrumb } from './hooks/useBreadcrumb';
