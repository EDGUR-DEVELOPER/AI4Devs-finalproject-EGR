
# 📂 Ficha del proyecto
## Nombre: Eduardo Guardado Ruiz
### 📌 Nombre del proyecto:
**SafeDocs Manager** (Document Management Security)

### 📌 Descripción breve:
Plataforma de gestión documental de alta seguridad con enfoque **API-First**, que integra cifrado, control de versiones lineal y un motor de búsqueda semántica basado en Inteligencia Artificial.

### 📌 Descripción general del producto:
SafeDocs Manager es una solución SaaS B2B diseñada como una **infraestructura documental inteligente**. No solo funciona como un repositorio seguro para sectores regulados (Legal, Fintech, Salud, RRHH), sino que actúa como un motor "backend" que permite a otros sistemas (ERPs, CRMs) heredar capacidades de seguridad avanzada. Combina una arquitectura **Zero-Trust** con accesibilidad programática mediante APIs RESTful, permitiendo la gestión del ciclo de vida del documento desde su creación y versionado hasta su recuperación mediante IA.

---

## 1. Objetivo del producto

El propósito principal de SafeDocs Manager DMS es resolver la dicotomía entre **seguridad extrema y facilidad de uso operativa**.

* **Propósito:** Mitigar el riesgo de fugas de información (Data Leaks) y eliminar el "Shadow IT" (uso de herramientas no autorizadas) causado por la complejidad de los sistemas tradicionales.
* **Problema que resuelve:** Permite la colaboración segura y la integración fluida entre sistemas aislados sin comprometer el cumplimiento normativo.
* **Segmentos de Usuario:**
    * **Administradores/CISO:** Responsables de seguridad y cumplimiento.
    * **Desarrolladores:** Integradores de sistemas terceros.
    * **Usuarios Finales (Ej. Abogados, RRHH):** Profesionales que requieren acceso rápido y fiable.
* **Valor Estratégico:** Provee seguridad de nivel gubernamental "invisible" para el usuario, potenciada por IA para la productividad y trazabilidad forense inmutable.

## 2. Características y funcionalidades principales

### A. Seguridad y Control de Acceso (Core)
* **Cifrado E2E & Zero-Trust:** Cifrado AES-256 de extremo a extremo; el sistema asume "cero confianza" por defecto.
* **RBAC Granular:** Control de acceso basado en roles (Ver, Editar, Descargar, Admin) aplicable a UI y API.
* **Audit Trails Inmutables:** Registro forense inalterable de cada acción (quién, cuándo, qué) sobre un archivo.
* **Marcas de Agua Dinámicas:** Inserción automática de identidad del usuario y fecha al visualizar documentos para prevenir fugas visuales.

### B. Gestión Documental Técnica
* **Control de Versiones Lineal:** Versionado (`v1.0` -> `v1.1`) con capacidad de "Rollback" y bloqueo (Check-in/Check-out) para edición segura.
* **Estructura de Carpetas Dinámica:** Organización jerárquica gestionable vía Web y API.

### C. Inteligencia Artificial y Búsqueda
* **Búsqueda Semántica (RAG/Vectorial):** Motor IA que entiende contexto y significado, no solo palabras clave exactas.
* **OCR Automático:** Extracción de texto de documentos escaneados e imágenes al subir.
* **Filtro de Seguridad en IA:** La IA respeta estrictamente los permisos RBAC; nunca revela datos restringidos en los resultados.

### D. Arquitectura de Integración (API-First)
* **API RESTful Estándar:** Endpoints documentados (OpenAPI/Swagger) para gestión de archivos, carpetas y permisos.
* **Gestión de API Keys:** Panel para creación y revocación de tokens para integraciones externas.
* **Webhooks:** Notificaciones push a sistemas terceros ante eventos (ej. documento firmado/actualizado).

## 3. Diseño y experiencia de usuario

### Perfil: Administrador / CISO
* **Entrada:** Dashboard centralizado con métricas de seguridad, consumo y alertas de actividad anómala.
* **Gestión:** Interfaz "Drag & Drop" para asignación de roles y permisos. Panel de control de API Keys con revocación instantánea.

### Perfil: Desarrollador
* **Onboarding:** Portal de documentación con Swagger UI interactivo.
* **Uso:** Estructuras JSON predecibles y códigos de error estándar para facilitar la integración.

### Perfil: Usuario Final (Operativo)
* **Navegación:** Interfaz limpia similar a exploradores nativos, con indicadores visuales de seguridad (candados, marcas de agua).
* **Interacción Principal:** Búsqueda en lenguaje natural ("contratos de junio") con resultados contextuales y previsualización segura.
* **Alertas:** Avisos claros sobre versiones obsoletas con redirección a la versión vigente.