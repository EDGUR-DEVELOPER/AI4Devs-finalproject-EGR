# AI4Devs-finalproject-EGR
# 0. Ficha del proyecto
## 0.1. Tu nombre completo:
## 0.2. Nombre del proyecto:
## 0.3. Descripción breve del proyecto:
## 0.4. URL del proyecto:

    Puede ser pública o privada, en cuyo caso deberás compartir los accesos de manera segura. Puedes enviarlos a alvaro@lidr.co usando algún servicio como onetimesecret.

## 0.5. URL o archivo comprimido del repositorio

    Puedes tenerlo alojado en público o en privado, en cuyo caso deberás compartir los accesos de manera segura. Puedes enviarlos a alvaro@lidr.co usando algún servicio como onetimesecret. También puedes compartir por correo un archivo zip con el contenido

# 1. Descripción general del producto

    Describe en detalle los siguientes aspectos del producto:

## 1.1. Objetivo:

    Propósito del producto. Qué valor aporta, qué soluciona, y para quién.

## 1.2. Características y funcionalidades principales:

    Enumera y describe las características y funcionalidades específicas que tiene el producto para satisfacer las necesidades identificadas.

## 1.3. Diseño y experiencia de usuario:

    Proporciona imágenes y/o videotutorial mostrando la experiencia del usuario desde que aterriza en la aplicación, pasando por todas las funcionalidades principales.

## 1.4. Instrucciones de instalación:

    Documenta de manera precisa las instrucciones para instalar y poner en marcha el proyecto en local (librerías, backend, frontend, servidor, base de datos, migraciones y semillas de datos, etc.)

# 2. Arquitectura del Sistema
## 2.1. Diagrama de arquitectura:

    Usa el formato que consideres más adecuado para representar los componentes principales de la aplicación y las tecnologías utilizadas. Explica si sigue algún patrón predefinido, justifica por qué se ha elegido esta arquitectura, y destaca los beneficios principales que aportan al proyecto y justifican su uso, así como sacrificios o déficits que implica.

## 2.2. Descripción de componentes principales:

    Describe los componentes más importantes, incluyendo la tecnología utilizada

## 2.3. Descripción de alto nivel del proyecto y estructura de ficheros

    Representa la estructura del proyecto y explica brevemente el propósito de las carpetas principales, así como si obedece a algún patrón o arquitectura específica.

## 2.4. Infraestructura y despliegue

    Detalla la infraestructura del proyecto, incluyendo un diagrama en el formato que creas conveniente, y explica el proceso de despliegue que se sigue

## 2.5. Seguridad

    Enumera y describe las prácticas de seguridad principales que se han implementado en el proyecto, añadiendo ejemplos si procede

## 2.6. Tests

    Describe brevemente algunos de los tests por realizar

# 3. Modelo de Datos
## 3.1. Diagrama del modelo de datos:

    Recomendamos usar mermaid para el modelo de datos, y utilizar todos los parámetros que permite la sintaxis para dar el máximo detalle, por ejemplo las claves primarias y foráneas.

## 3.2. Descripción de entidades principales:

    Recuerda incluir el máximo detalle de cada entidad, como el nombre y tipo de cada atributo, descripción breve si procede, claves primarias y foráneas, relaciones y tipo de relación, restricciones (unique, not null…), etc.

# 4. Especificación de la API

    Si tu backend se comunica a través de API, describe los endpoints principales (máximo 3) en formato OpenAPI. Opcionalmente puedes añadir un ejemplo de petición y de respuesta para mayor claridad

# 5. Historias de Usuario

    Documenta 3 de las historias de usuario principales utilizadas durante el desarrollo, teniendo en cuenta las buenas prácticas de producto al respecto.

* Historia de Usuario 1
* Historia de Usuario 2
* Historia de Usuario 3

# 6. Tickets de Trabajo

    Documenta 3 de los tickets de trabajo principales del desarrollo, uno de backend, uno de frontend, y uno de bases de datos. Da todo el detalle requerido para desarrollar la tarea de inicio a fin teniendo en cuenta las buenas prácticas al respecto.

* Ticket 1
* Ticket 2
* Ticket 3

---

# 📂 Documentación de Producto: SentinelCore DMS

### 📌 Nombre del proyecto:
**SentinelCore DMS** (Document Management Security)

### 📌 Descripción breve:
Plataforma de gestión documental de alta seguridad con enfoque **API-First**, que integra cifrado, control de versiones lineal y un motor de búsqueda semántica basado en Inteligencia Artificial.

### 📌 Descripción general del producto:
SentinelCore es una solución SaaS B2B diseñada como una **infraestructura documental inteligente**. No solo funciona como un repositorio seguro para sectores regulados (Legal, Fintech, Salud, RRHH), sino que actúa como un motor "backend" que permite a otros sistemas (ERPs, CRMs) heredar capacidades de seguridad avanzada. Combina una arquitectura **Zero-Trust** con accesibilidad programática mediante APIs RESTful, permitiendo la gestión del ciclo de vida del documento desde su creación y versionado hasta su recuperación mediante IA.

---

## 1. Objetivo del producto

El propósito principal de SentinelCore DMS es resolver la dicotomía entre **seguridad extrema y facilidad de uso operativa**.

* **Propósito:** Mitigar el riesgo de fugas de información (Data Leaks) y eliminar el "Shadow IT" (uso de herramientas no autorizadas) causado por la complejidad de los sistemas tradicionales.
* **Problema que resuelve:** Permite la colaboración segura y la integración fluida entre sistemas aislados sin comprometer el cumplimiento normativo.
* **Segmentos de Usuario:**
    * **Administradores/CISO:** Responsables de seguridad y cumplimiento.
    * **Desarrolladores:** Integradores de sistemas terceros.
    * **Usuarios Finales (Abogados, RRHH):** Profesionales que requieren acceso rápido y fiable.
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