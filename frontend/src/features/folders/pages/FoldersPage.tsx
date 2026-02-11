/**
 * FoldersPage - Página principal de gestión de carpetas
 * Incluye layout con sidebar, acciones rápidas y estadísticas
 */
import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { DashboardLayout } from '@features/dashboard/components';
import { FolderExplorer } from '../components/FolderExplorer';
import { useFolderContent } from '../hooks/useFolderContent';
import { Button } from '@ui/forms/Button';

/**
 * Página principal de gestión de carpetas
 * Envuelve FolderExplorer con DashboardLayout y agrega sidebar con acciones
 */
export const FoldersPage: React.FC = () => {
    const { id: folderId } = useParams<{ id: string }>();
    const { data: content, isLoading } = useFolderContent(folderId);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    // @ts-expect-error - isUploadModalOpen se usará cuando se implemente DocumentUploadModal (US-DOC-006)
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    // Datos para estadísticas (con valores por defecto seguros)
    const totalCarpetas = content?.subcarpetas?.length || 0;
    const totalDocumentos = content?.documentos?.length || 0;
    const permisos = content?.permisos || { puede_leer: false, puede_escribir: false, puede_administrar: false };

    // Estado de carga
    if (isLoading) {
        return (
            <DashboardLayout>
                <div className="flex items-center justify-center py-16">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                    <span className="ml-3 text-gray-600">Cargando carpetas...</span>
                </div>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout>
            {/* Main Layout: Content + Sidebar */}
            <div className="flex flex-col lg:flex-row gap-6">
                {/* Main Content (70%) */}
                <div className="flex-1 lg:w-2/3">
                    <FolderExplorer
                        isCreateModalOpen={isCreateModalOpen}
                        onCreateModalChange={setIsCreateModalOpen}
                        hideCreateButton={false}
                    />
                </div>

                {/* Sidebar (30%) */}
                <aside className="lg:w-1/3 space-y-4">
                    {/* Acciones Rápidas */}
                    <div className="bg-white rounded-lg border border-gray-200 p-4">
                        <h3 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                            <svg className="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                            Acciones Rápidas
                        </h3>
                        <div className="space-y-2">
                            {permisos.puede_escribir && (
                                <>
                                    <Button
                                        onClick={() => setIsCreateModalOpen(true)}
                                        variant="primary"
                                        fullWidth
                                    >
                                        + Nueva Carpeta
                                    </Button>
                                    <Button
                                        onClick={() => setIsUploadModalOpen(true)}
                                        variant="primary"
                                        fullWidth
                                    >
                                        📤 Subir Documento
                                    </Button>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Estadísticas */}
                    <div className="bg-white rounded-lg border border-gray-200 p-4">
                        <h3 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                            <svg className="w-4 h-4 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                            </svg>
                            Estadísticas
                        </h3>
                        <div className="space-y-3">
                            <div className="flex items-center justify-between">
                                <span className="text-sm text-gray-600">📁 Carpetas</span>
                                <span className="text-lg font-semibold text-gray-900">{totalCarpetas}</span>
                            </div>
                            <div className="flex items-center justify-between">
                                <span className="text-sm text-gray-600">📄 Documentos</span>
                                <span className="text-lg font-semibold text-gray-900">{totalDocumentos}</span>
                            </div>
                            <div className="pt-2 border-t border-gray-200">
                                <span className="text-xs text-gray-500">En carpeta actual</span>
                            </div>
                        </div>
                    </div>

                    {/* Permisos */}
                    <div className="bg-white rounded-lg border border-gray-200 p-4">
                        <h3 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                            <svg className="w-4 h-4 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                            </svg>
                            Tus Permisos
                        </h3>
                        <div className="space-y-2">
                            <div className="flex items-center gap-2">
                                <span className={`w-2 h-2 rounded-full ${permisos.puede_leer ? 'bg-green-500' : 'bg-gray-300'}`}></span>
                                <span className="text-sm text-gray-700">Lectura</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className={`w-2 h-2 rounded-full ${permisos.puede_escribir ? 'bg-green-500' : 'bg-gray-300'}`}></span>
                                <span className="text-sm text-gray-700">Escritura</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className={`w-2 h-2 rounded-full ${permisos.puede_administrar ? 'bg-green-500' : 'bg-gray-300'}`}></span>
                                <span className="text-sm text-gray-700">Administración</span>
                            </div>
                        </div>
                    </div>

                    {/* Ayuda */}
                    <div className="bg-blue-50 rounded-lg border border-blue-200 p-4">
                        <h3 className="text-sm font-semibold text-blue-900 mb-2 flex items-center gap-2">
                            <svg className="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            Consejos
                        </h3>
                        <ul className="text-xs text-blue-800 space-y-1">
                            <li>• Haz clic en una carpeta para explorar su contenido</li>
                            <li>• Usa el breadcrumb para navegar entre niveles</li>
                            <li>• Solo carpetas vacías pueden eliminarse</li>
                        </ul>
                    </div>
                </aside>
            </div>
                            
        </DashboardLayout>
    );
};
