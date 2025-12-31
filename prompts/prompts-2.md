## Tickets
`Modificacion de Prompt para generar la lista de tickets por epicas`
```
A partir de la siguientes Historias de Usuarios, genera por separado listas completas de tickets necesarios para implementar la funcionalidad desde inicio a fin.

Lista de Historias de Usuario en la Epica:
[Indicar epica con historias de usuario aquí]

Tu tarea:
– Analizar las historias.
– Identificar sus capacidades principales.
– Dividir la funcionalidad en tickets accionables y entregables independientes.
– NO inventar funcionalidades nuevas; solo descomponer la historia.

Estructura obligatoria de salida:

1. Resumen de alcance detectado

– Lista de capacidades encontradas
– Restricciones implícitas
– Riesgos o ambigüedades (si existen)

2. Lista de tickets necesarios (granular, ordenados)

Cada ticket debe estar estructurado así:

Título: [Acción breve]
Objetivo: [Qué resuelve]
Tipo: [Historia / tarea / subtarea / bug / diseño / QA]
Descripción corta: [Máx. 3–4 líneas]
Entregables:
– [Entregable 1]
– [Entregable 2]

(Generar tantos como sean necesarios para completar la historia.)

3. Flujo recomendado de ejecución

– Orden ideal de implementación
– Dependencias entre tickets

4. Recomendación TDD/BDD

– Qué tickets deberían tener pruebas primero
– Qué tickets se prestan a escenarios BDD

5. Generacion de archivos md

- Genera lista de archivos md con el nombre de la US
- Este archivo contendra la lista de tickets por cada US
- Utiliza como ejemplo el contenido de la carpeta [Indicacion de la carpeta]

Reglas del prompt:
– No agregar funcionalidades nuevas que no existan en la historia.
– Si la historia es muy grande, divídela en varias historias y luego en tickets.
– Usa lenguaje claro para producto, diseño, desarrollo y QA.
– Asegura que cada ticket sea independiente, estimable y verificable.
– Prioriza granularidad útil: ni demasiado grande ni demasiado pequeña.
- Dividir los tickets en Base de datos, Backend y Frontend.
- Coloca los archivos en la carpeta [Inidicacion del nombre de la carpeta].
```
"

## Inicializar proyectos
`Modelo creacion meta prompts: "GPT-5.1-Codex-Max". Se realiza modificaciones manualmente.`
`Modelo para ejecutar meta prompts: "Claude Opus 4.5"`

### Backend
#### IAM
```prompt
Como experto desarrollo backend, Quiero que generes un proyecto backend IAM (wrapper ligero de Keycloak) listo para clonar y ejecutar localmente. Sigue estas instrucciones al pie de la letra:

Stack: Java 21, Spring Boot 3.5.x (última 3.5.x), Maven, empaquetado JAR ejecutable.
Dependencias exactas (solo estas): spring-boot-starter-web, spring-boot-starter-test, spring-boot-starter-data-jpa (para specs), spring-boot-starter-validation, MapStruct, springdoc-openapi, Lombok. No añadas otras.
No configures bases de datos ni seguridad en esta etapa (sin datasources, sin Keycloak config aún).
Código de ejemplo: crea un controlador REST “Hello World” en el paquete com.docflow.identity.
Archivos obligatorios completos: pom.xml, src/main/java/... con clase principal y el controlador, src/main/resources/application.yml con configuración mínima (nombre de app, puerto), README.md con pasos para compilar, probar y ejecutar vía Maven/Java 21, estructura de directorios incluida.
Usa versiones estables y actuales para todas las dependencias dentro de la rama Spring Boot 3.5.x.
Salida: entrega todo el árbol del proyecto con contenido completo de cada archivo, listo para compilar y correr con mvn spring-boot:run
```

#### Document Core
```prompt
Como experto desarrollo backend, Quiero que generes un proyecto backend “Document Core Service” para DocFlow, listo para clonar y ejecutar localmente. Sigue estas instrucciones al pie de la letra:

Stack: Java 21, Spring Boot 3.5.x (última 3.5.x), Maven, empaquetado JAR ejecutable.
Dependencias exactas (solo estas): spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, spring-boot-starter-test, Lombok, MapStruct, springdoc-openapi-starter-webmvc-ui. No añadas otras.
Sin seguridad ni bases de datos configuradas todavía: no declares datasources ni proveedores de seguridad; deja preparado para añadirlos después.
Código de ejemplo mínimo:
Clase principal en com.docflow.documentcore.
Controlador REST HelloController en com.docflow.documentcore con endpoint GET /hello que devuelva { "message": "Hello Document Core" }.
Archivos obligatorios completos: pom.xml, clase principal, controlador, application.yml con configuración mínima (nombre de app, server.port), README.md con pasos para compilar, probar y ejecutar (mvn spring-boot:run), y estructura de directorios.
Usa versiones estables y actuales dentro de la rama Spring Boot 3.5.x.
Salida: entrega el árbol del proyecto con el contenido completo de cada archivo, listo para ejecutar con Maven y Java 21.
```

#### AuditLog 
```prompt
Como experto desarrollo backend, Quiero que generes un proyecto backend “Audit Log Service” para DocFlow, listo para clonar y ejecutar localmente. Sigue estas instrucciones al pie de la letra:

Stack: Java 21, Spring Boot 3.5.x (última 3.5.x), WebFlux reactivo, Maven, empaquetado JAR ejecutable.
Dependencias exactas (solo estas): spring-boot-starter-webflux, spring-boot-starter-validation, spring-boot-starter-test, springdoc-openapi-starter-webflux-ui, spring-boot-starter-data-mongodb-reactive (para futura persistencia), Lombok. No añadas otras.
Sin seguridad ni configuración de base de datos todavía: no declares credenciales ni URIs; deja el espacio listo para añadirlos después.
Código de ejemplo mínimo:
Clase principal en com.docflow.audit.
Controlador REST HealthController en com.docflow.audit con endpoint GET /health que devuelva { "status": "ok" }.
Archivos obligatorios completos: pom.xml, clase principal, controlador, application.yml con configuración mínima (nombre de app, server.port), README.md con pasos para compilar, probar y ejecutar (mvn spring-boot:run), y estructura de directorios.
Usa versiones estables y actuales dentro de la rama Spring Boot 3.5.x; alinea SpringDoc y las dependencias reactivas con esa versión.
Salida: entrega el árbol del proyecto con el contenido completo de cada archivo, listo para ejecutar con Maven y Java 21.
```

#### Message Broker
```prompt
Como experto desarrollo backend, Quiero que generes un proyecto backend “Message Broker Service” para DocFlow, listo para clonar y ejecutar localmente. Sigue estas instrucciones al pie de la letra:

Stack: Java 21, Spring Boot 3.5.x (última 3.5.x), Maven, empaquetado JAR ejecutable.
Dependencias exactas (solo estas): spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-test, spring-kafka, springdoc-openapi-starter-webmvc-ui, Lombok. No añadas otras.
Sin seguridad ni bases de datos en esta fase.
Código de ejemplo mínimo:
Clase principal en com.docflow.broker.
Controlador REST HealthController con GET /health que devuelva { "status": "ok" }.
Controlador REST PublishController con POST /publish que reciba { "topic": "...", "message": "..." } y envíe el mensaje usando un KafkaTemplate<String, String>.
Listener de ejemplo DemoListener que consuma de un tópico configurable (ej. ${broker.demo-topic:demo-topic}) y registre el mensaje por log.
Configuración mínima en application.yml: server.port, spring.application.name, placeholders para spring.kafka.bootstrap-servers y el tópico demo.
README.md con pasos para compilar, probar y ejecutar (mvn spring-boot:run), más cómo probar el publish con curl.
Usa versiones estables y actuales dentro de la rama Spring Boot 3.5.x; alinea spring-kafka y springdoc con esa versión.
Salida: entrega el árbol del proyecto con el contenido completo de cada archivo, listo para ejecutar con Maven y Java 21.
```

#### Gateway API
```prompt
Como experto desarrollo backend, Quiero que generes un proyecto “API Gateway” para DocFlow, listo para clonar y ejecutar localmente. Sigue estas instrucciones al pie de la letra:

Stack: Java 21, Spring Boot 3.5.x (última 3.5.x), Spring Cloud Gateway 2023.x compatible, Maven, empaquetado JAR ejecutable.
Dependencias exactas (solo estas): spring-boot-starter-webflux, spring-cloud-starter-gateway, spring-boot-starter-validation, spring-boot-starter-test, springdoc-openapi-starter-webflux-ui, Lombok. No añadas otras.
Sin seguridad ni datasources en esta fase: no configures OAuth2/Keycloak ni bases de datos.
Routing mínimo de ejemplo:
/api/iam/** -> http://localhost:8081, con stripPrefix(2).
/api/doc/** -> http://localhost:8082, con stripPrefix(2).
Filtro global: añade header X-DocFlow-Gateway: v1 en todas las respuestas.
Código mínimo:
Clase principal en com.docflow.gateway.
Configuración de rutas vía bean RouteLocator.
Controlador HealthController con GET /health → { "status": "ok" }.
application.yml: server.port=8080, spring.application.name=gateway, configuración de rutas y swagger-ui habilitado.
README.md: pasos para compilar, probar y ejecutar (mvn spring-boot:run), y cómo probar /health.
Usa versiones estables alineadas con Spring Boot 3.5.x y Spring Cloud 2023.x.
Salida: entrega el árbol del proyecto con el contenido completo de cada archivo, listo para ejecutar con Maven y Java 21.
```
### Frontend
```prompt
Actúa como un **Arquitecto de Software Principal** y un experto en buenas prácticas de desarrollo Frontend.

Tu objetivo es generar una guía completa y el código inicial para un proyecto de **React + Vite + TypeScript** siguiendo una **Arquitectura Híbrida Feature-Driven** combinada con principios de **Clean Architecture** (separación de la lógica de negocio pura de la infraestructura y la UI).

**STACK TECNOLÓGICO Y ESTÁNDARES:**
1.  **Core:** React 18+, Vite, TypeScript (Strict Mode).
2.  **UI:** Tailwind CSS (configuración completa).
3.  **Estado/Lógica:** Zustand.
4.  **Infraestructura/Datos:** Axios (instancia singleton).
5.  **Routing:** React Router DOM v6.

**REQUISITOS DE ARQUITECTURA (FEATURE-DRIVEN CLEAN):**

1.  **Estructura Base:** La arquitectura debe estar segmentada en la raíz de `/src` de la siguiente manera:
    * `/src/core/`: Contiene la lógica **pura** (Domain y Shared).
        * `/src/core/domain`: Interfaces, tipos y modelos de datos. (Puro, sin dependencias externas).
        * `/src/core/shared`: Constantes, utilidades, configuraciones globales (Axios instance, Router base).
    * `/src/features/`: El corazón de la arquitectura Feature-Driven. Cada carpeta es una funcionalidad completa.
        * `/src/features/feature-name/api`: Lógica de peticiones (Axios calls) para la feature.
        * `/src/features/feature-name/components`: Componentes internos de la feature.
        * `/src/features/feature-name/hooks`: Lógica y gestión de estado (Zustand store) de la feature.
        * `/src/features/feature-name/pages`: Páginas que orquestan la feature.
    * `/src/common/ui/`: Componentes atómicos o de diseño reutilizables globalmente (Ej: Botón, Layouts, etc.).

2.  **Principios de Clean Code:**
    * Utiliza **Alias de Ruta** (`@core`, `@features`, `@ui`) en `vite.config.ts` y `tsconfig.json`.
    * Implementa **Barrel Exports** (`index.ts`) en las carpetas de features para mantener las importaciones limpias.

**ENTREGABLES SOLICITADOS:**

### 1. Inicialización y Comandos
* Proporciona el script completo de `bash` para crear el proyecto e instalar **todas las dependencias** necesarias.

### 2. Configuración y Código Base
* Contenido de `tailwind.config.js`.
* Contenido de `index.css` con las directivas de Tailwind.
* Código de la instancia **Axios** (`src/core/shared/api/axiosInstance.ts`).
* Configuración del **Router** (`src/core/shared/router/AppRouter.tsx`).

### 3. Implementación de una Feature de Ejemplo
* Genera una feature de ejemplo llamada **`UserManagement`**.
* Proporciona el código completo de la **Interfaz de Dominio** (`User.ts`), la **Lógica de Fetching** (usando Axios en la carpeta `api`), el **Store de Zustand** (`hooks/useUserStore.ts`), y la **Página de Vista** (`pages/UserListPage.tsx`).

### 4. Archivo README.md (Obligatorio)
* Genera un archivo `README.md` en formato Markdown que incluya:
    * Título y Breve Descripción del Proyecto.
    * **Tecnologías Utilizadas**.
    * **Arquitectura Implementada** (Mencionando Feature-Driven).
    * Sección de **Instalación y Levantamiento** con los comandos exactos (`npm install`, `npm run dev`).
    * Sección de **Estructura de Carpetas Clave** (explicando la función de `/core`, `/features` y `/common/ui`).

**NOTA FINAL:** Asegura que la arquitectura impida que el código de la UI acceda directamente a los detalles de implementación de la API. Todo debe fluir a través de los hooks y la capa de estado (Zustand).
```