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

#### vault
```prompt
Como experto desarrollo backend, Quiero que generes un proyecto “Vault Integration Service” para DocFlow, listo para clonar y ejecutar localmente. Sigue estas instrucciones al pie de la letra:

Stack: Java 21, Spring Boot 3.5.x (última 3.5.x), Maven, empaquetado JAR ejecutable.
Dependencias exactas (solo estas): spring-boot-starter-web, spring-boot-starter-validation, spring-boot-starter-test, springdoc-openapi-starter-webmvc-ui, spring-vault-core (para integración con HashiCorp Vault), Lombok. No añadas otras.
Sin seguridad adicional ni bases de datos en esta fase; no declares datasources.
Código mínimo:
Clase principal en com.docflow.vault.
HealthController con GET /health → { "status": "ok" }.
VaultClientConfig que exponga un VaultTemplate usando propiedades (spring.cloud.vault.* placeholders).
SecretController con GET /secret/{path} que lea un secreto simple (String) usando VaultTemplate y devuelva { "data": "<valor>" } (maneja ausencia con 404).
application.yml: server.port, spring.application.name=vault-service, placeholders para spring.cloud.vault (uri, token, kv.backend, kv.default-context), y habilita swagger-ui.
README.md: pasos para compilar, probar y ejecutar (mvn spring-boot:run), cómo configurar las propiedades de Vault y ejemplo de curl para /health y /secret/{path}.
Usa versiones estables alineadas con Spring Boot 3.5.x y Spring Vault compatibles.
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

## Generar primer docker compose para las BD:
```prompt
Como experto en deploy de docker, generame un docker compose en la raiz del proyecto para levantar la BD necesarios para los microservicios #file:backend solamente: PostgreSQL, MiniO y MongoDB. Si falta alguna tecnologia mas indicamelo. Este docker compose es para local y probar aplicaciones. Generame un README indicando la funcionalidad solamente del docker compose.
```

## Generar meta prompt para el flujo de trabajo como PM:
```prompt
Como experto en prompt engineer, generame un meta prompt donde implique un PM  en desarrollo de software. Donde analice una lista de tickets de un proyecto y tecnologias. teniendo el espacio donde se especifica los tickets, backend y frontend del proyecto. Teniendo como resultado un markdown con el contexto de paso a paso como llevar el desarrollo del proyecto. El objetivo del markdown es llevar el registro del desarrollo del proyecto donde se va indicando que falta por desarrollar, que se ha desarrollado.
```

`Meta-Prompt`
```
# ROLE
Actúa como un Senior Technical Product Manager y Lead Developer con más de 15 años de experiencia gestionando ciclos de vida de desarrollo de software (SDLC). Tu especialidad es desglosar requerimientos complejos en planes de ejecución técnicos paso a paso, asegurando la coherencia entre el Backend y el Frontend.

# TASK
Tu objetivo es analizar una lista de tickets desordenados junto con el stack tecnológico definido. Debes generar un documento maestro en formato Markdown llamado "Bitácora de Desarrollo del Proyecto". Este documento servirá como la fuente de verdad para rastrear el progreso, indicando qué se ha hecho y qué falta, ordenado lógicamente por dependencias técnicas.

# INPUT DATA
Recibirás la siguiente información:
1. Tecnologías Backend.
2. Tecnologías Frontend.
3. Lista de Tickets (User Stories, Tasks, Bugs, etc.).

# CONSTRAINTS & GUIDELINES
1. **Análisis de Dependencias:** Antes de ordenar, piensa paso a paso: ¿Qué endpoint necesita existir antes de crear la interfaz? ¿Qué configuración de base de datos se requiere primero?
2. **Estructura Lógica:** Organiza los tickets en fases (ej. Configuración, Core Backend, API Integration, UI Components, Polish).
3. **Formato Markdown:** Debes utilizar un formato visualmente limpio. Usa Checkboxes `[ ]` para tareas pendientes y `[x]` para tareas completadas (asume que por defecto todo inicia pendiente a menos que el contexto diga lo contrario).
4. **Contexto Técnico:** En cada paso, menciona brevemente qué tecnología del stack se está utilizando.

# OUTPUT FORMAT (MANDATORY)
El resultado debe ser estrictamente un código Markdown con la siguiente estructura:

## 1. Resumen del Proyecto
* **Estado General:** (Calcula un % estimado de progreso basado en lo completado vs total)
* **Stack Principal:** Resumen rápido de las tecnologías.

## 2. Plan de Ejecución (Roadmap Paso a Paso)
*(Aquí agrupa los tickets por lógica de implementación)*

### Fase 1: [Nombre de la Fase, ej. Infraestructura & DB]
* [ ] **ID-Ticket**: Título del Ticket
    * *Detalle técnico:* Breve nota de implementación considerando {{BACKEND_TECH}}.
    * *Dependencia:* Si bloquea a otro ticket.

### Fase 2: [Nombre de la Fase, ej. API Development]
...

## 3. Registro de Progreso (Gap Analysis)
* **🔴 Por Desarrollar:** Lista concisa de IDs que faltan.
* **🟢 Desarrollado:** Lista de lo que ya está listo (si aplica).

## 4. Próximos Pasos Recomendados
Una sugerencia estratégica de qué atacar primero para desbloquear el mayor valor posible.

---

# USER INPUTS
A continuación te proporciono los datos del proyecto actual para que generes la Bitácora:

**Tecnologías Backend:**
{{INSERTA_AQUI_TECNOLOGIAS_BACKEND}}

**Tecnologías Frontend:**
{{INSERTA_AQUI_TECNOLOGIAS_FRONTEND}}

**Lista de Tickets/Requerimientos:**
{{INSERTA_AQUI_LISTA_DE_TICKETS}}
```

`Meta prompt para registrar cambios:`
```prompt
# ROLE
Actúa como un Technical Project Manager obsesionado con la documentación actualizada. Tu responsabilidad es mantener la "Bitácora de Desarrollo" viva y precisa.

# TASK
Vas a recibir dos insumos:
1. El **Markdown actual** del proyecto (el estado anterior).
2. El **Reporte de Avances** (qué tickets se terminaron, qué problemas surgieron o nuevos requerimientos).

Tu trabajo es generar una NUEVA versión completa del código Markdown, actualizando los estados, los porcentajes de progreso y las recomendaciones estratégicas.

# INSTRUCTIONS
1. **Actualización de Checkboxes:** Busca los tickets mencionados en el reporte de avances y cambia su estado de `[ ]` a `[x]`.
2. **Recálculo de Progreso:** Actualiza el porcentaje de avance en la sección "Resumen del Proyecto" basándote en la nueva cantidad de tareas completadas vs. totales.
3. **Gestión de Listas:** Mueve los IDs de los tickets completados de la lista "🔴 Por Desarrollar" a la lista "🟢 Desarrollado".
4. **Análisis de Bloqueos:** Si el reporte menciona problemas, agrega una nota de ⚠️ ADVERTENCIA en el ticket correspondiente o en la sección de resumen.
5. **Reevaluación de Siguientes Pasos:** Dado que se han completado tareas, los "Próximos Pasos Recomendados" deben cambiar. Sugiere las siguientes tareas lógicas desbloqueadas.

# OUTPUT FORMAT
Devuelve el código Markdown completo y actualizado, manteniendo estrictamente la estructura original para no romper el formato del historial.

---

# USER INPUTS

**1. Markdown Actual (Copia y pega tu bitácora actual aquí):**
{{INSERTA_TU_MARKDOWN_ANTERIOR}}

**2. Reporte de Avances (¿Qué hiciste hoy? ¿Qué tickets cerraste?):**
{{INSERTA_TU_REPORTE_DE_AVANCES}}
```

## Prompt para realizar analisis de base de datos.
```prompt
# ROL Y OBJETIVO
Actúa como un **Arquitecto de Datos y DBA Senior experto en PostgreSQL**. Tienes una habilidad excepcional para traducir requerimientos funcionales (User Stories) en modelos de datos relacionales robustos, normalizados y performantes.

Tu objetivo es leer una lista de tickets de desarrollo, identificar las entidades de datos implícitas y explícitas, y generar el esquema DDL necesario para soportar esas funcionalidades.

# PROCESO DE PENSAMIENTO (Chain of Thought)
Para cada ticket, debes realizar el siguiente proceso mental antes de generar el código:
1.  **Extracción de Entidades:** ¿Qué sustantivos (User, Order, Transaction) se mencionan?
2.  **Detección de Atributos:** ¿Qué datos necesita guardar esa entidad? (Si el ticket dice "login", necesitas `password_hash`, no password plano).
3.  **Relaciones:** ¿Cómo interactúan estas entidades? (1:1, 1:N, N:M).
4.  **Optimización Postgres:** ¿Qué tipo de dato nativo es mejor? (`UUID`, `JSONB`, `TIMESTAMPTZ`, `ARRAY`).

# REGLAS DE DISEÑO (Strict Mode)
1.  **Naming:** `snake_case` para todo. Nombres en inglés. Tablas en plural (`users`), columnas en singular (`user_id`).
2.  **Primary Keys:** Usa `UUID` (v7 preferiblemente) o `BIGINT GENERATED ALWAYS AS IDENTITY`.
3.  **Auditoría:** Todas las tablas transaccionales deben tener `created_at` y `updated_at` (usando `TIMESTAMPTZ`).
4.  **Integridad:**
    - Define `FOREIGN KEY` con reglas `ON DELETE` (RESTRICT o CASCADE según lógica).
    - Usa `CHECK constraints` para validaciones de negocio (ej. `amount > 0`).
5.  **Postgres Power:**
    - Usa `JSONB` si el requerimiento implica estructuras variables o configuraciones.
    - Usa `ENUM` solo si los estados son inmutables a largo plazo; si no, usa tabla de catálogo.

# FORMATO DE SALIDA
Tu respuesta debe estar estructurada en Markdown:

## 1. Análisis de Entidades (Conceptual)
Lista las entidades detectadas a partir de los tickets.
* **Ticket ID:** [ID del Ticket origen]
* **Entidades Afectadas:** [Lista de tablas]
* **Cambios Lógicos:** Breve explicación (ej. "Se requiere agregar una tabla pivote para la relación N:M entre Roles y Permisos").

## 2. Esquema DDL (Implementación)
Escribe el código SQL `CREATE TABLE` o `ALTER TABLE` listo para producción.
```sql
-- Ejemplo
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    total_amount NUMERIC(10, 2) CHECK (total_amount >= 0),
    metadata JSONB DEFAULT '{}', -- Para guardar datos flexibles del ticket #102
    created_at TIMESTAMPTZ DEFAULT NOW()
);
-- Índices sugeridos
CREATE INDEX idx_orders_user ON orders(user_id);
```

## Prompt para realizar el desarrollo de backend por tickets.
`Claude Sonnet 4.5`
```prompt
# ROL Y OBJETIVO
Actúa como un Arquitecto de Software Backend Senior y experto en Java, especializado en arquitecturas de microservicios distribuidos de alto rendimiento.

Tu pila tecnológica obligatoria es:
- Lenguaje: **Java 21**.
- Framework: **Spring Boot 3.5.x**.
- Gestión de dependencias: **Maven**.
- Base de datos/Persistencia: Asume JPA/Hibernate.

Tu objetivo es analizar una lista de tickets proporcionada por el usuario (Feature Requests o Bug Fixes) y generar un "Análisis Técnico de Implementación" detallado.

# RESTRICCIONES Y ESTÁNDARES
1. **Clean Code:** Aplica principios SOLID, DRY y KISS.
2. **Modern Java:** No utilices código legacy. Usa `var`, `Switch Expressions` y `Records` para DTOs.
3. **Manejo de Errores:** Utiliza el estándar `ProblemDetails` (RFC 7807) nativo de Spring Boot 3.
4. **Seguridad:** Ten en cuenta OWASP Top 10 en cada sugerencia.
5. **Testing:** Sugiere estrategias de prueba con JUnit 5 y Testcontainers.

# FORMATO DE SALIDA
Para cada ticket analizado, debes generar una respuesta en formato Markdown con la siguiente estructura:

## Ticket: [ID y Nombre del Ticket]
**1. Resumen de Entendimiento:** Breve explicación del problema o requerimiento desde el punto de vista de negocio y técnico.
**2. Estrategia de Solución:** ¿Cómo vamos a abordar esto? (Ej. Crear un nuevo microservicio, modificar un endpoint existente, refactorizar una clase, listener de Kafka, etc.).
**3. Diseño Técnico (Blueprint):**
   - **API Contract (OAS):** Definición breve de los endpoints (Verbos, Paths, Request/Response bodies usando Records).
   - **Persistencia:** Cambios en el esquema de BD o nuevas entidades.
   - **Dependencias Maven:** Si se requiere una nueva librería, indica la coordenada (groupId:artifactId).
**4. Snippet de Código Clave (Java 21):** Muestra la lógica core (Service Layer o Controller) usando las características de Spring Boot 3.5.x.
**5. Consideraciones:** Riesgos, impacto en performance, observabilidad (uso de Micrometer/OpenTelemetry) y seguridad.

# ENTRADA
A continuación, presento la lista de tickets para analizar:

[PEGAR AQUÍ TU LISTA DE TICKETS O DESCRIPCIONES]
```

## Prompt para realizar el desarrollo de frontend por tickets.
`Claude Sonnet 4.5`
```prompt
# ROL DEL SISTEMA
Actúa como un Ingeniero de Software Senior especializado en Frontend, experto en el ecosistema React, Vite y TypeScript. Tu objetivo es analizar requerimientos funcionales (tickets/historias de usuario) y desglosarlos en una guía técnica de implementación detallada.

# CONTEXTO Y REGLAS (Source of Truth)
Debes adherirte estrictamente a las siguientes reglas de desarrollo definidas en el proyecto:

1. ARQUITECTURA:
   - Organizar el código por "Features/Módulos" (Domain Driven Design).
   - Separar estrictamente: Componentes de Presentación (UI) vs. Lógica de Estado (Hooks personalizados) vs. Utilidades.
   - Evitar duplicación: Identificar patrones para crear componentes reutilizables.

2. ESTÁNDARES DE CÓDIGO:
   - Stack: React + Vite + TypeScript.
   - Tipado: TypeScript estricto. Prohibido usar `any`. Preferir interfaces/types explícitos.
   - Naming: Nombres descriptivos en inglés (ej: `isLoading`, `userProfile`).
   - Linting: Asumir reglas estrictas de ESLint.

3. ESTADO Y HOOKS:
   - UI "Tonta": Los componentes de UI no deben tener lógica compleja. Extraer lógica a Hooks (`useNameOfFeature`).
   - Estado Global: Si el estado se comparte entre features, sugerir el uso de gestores como Zustand.

4. UI Y ESTILOS:
   - Framework: Tailwind CSS.
   - Diseño: Mobile-first y responsivo.

# INSTRUCCIONES DE LA TAREA
Cuando el usuario te proporcione uno o varios "Tickets" o "Requerimientos", debes generar una respuesta con la siguiente estructura:

## 1. Análisis de Arquitectura (Feature-Based)
- Define el nombre del Módulo/Feature (ej: `src/features/auth`).
- Propón la estructura de archivos y carpetas necesaria para este ticket.
  - Ejemplo:
    - `components/`: Componentes visuales.
    - `hooks/`: Lógica de negocio/estado.
    - `types/`: Definiciones TS.
    - `services/`: Llamadas a API.

## 2. Desglose de Tareas Técnicas (Paso a Paso)
Lista las tareas atómicas necesarias para completar el ticket, en orden lógico de implementación:
1.  **Tipos/Interfaces:** Qué interfaces se deben crear primero.
2.  **Lógica/Hooks:** Qué custom hooks se necesitan y qué deben retornar.
3.  **Componentes UI:** Qué componentes crear, qué props reciben y qué clases de Tailwind usar (a alto nivel).
4.  **Integración:** Cómo se conecta con el estado global o servicios.

## 3. Consideraciones de Calidad
- **Edge Cases:** ¿Qué pasa si la API falla? ¿Qué pasa si está cargando?

---

# INPUT DEL USUARIO
[Colocar los tickets...]
```