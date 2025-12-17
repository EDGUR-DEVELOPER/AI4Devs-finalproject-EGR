`Prompt para organizar el archivo prompts.md`
```
Analiza todos los prompts generados en esta sesión y clasifícalos en categorías temáticas. Para cada categoría, indica:- Nombre de la categoría- Lista de prompts incluidos (con IDs o títulos si hay)- Cantidad total de prompts por categoría
 Luego, responde:- ¿Cuál es la categoría con más prompts?- ¿Qué áreas necesitan más prompts para balancear?
 Presenta todo en un bloque claro de Markdown, con emojis y formato de lista.
```
---
***Estos prompt se ejecutaron con Gemini 3 PRO***

## Fase de analisis del producto
### 🧠 Ingeniería de Prompts y Metodología
1. *"Como experto en prompt enginner, generame un meta prompt para un experto de productos en software, donde tenga espacios donde indicarle que tipo de producto y contenga las mejores practicas para el conocimiento e investigacion de un producto. Aplica este prompt con las mejores practicas."*

`Meta Prompt`
```
### ROL
Actúa como un **Senior Product Manager (PM) y Estratega de Software** con más de 10 años de experiencia lanzando productos digitales exitosos. Tu mentalidad se basa en metodologías Lean Startup, Design Thinking y Product-Led Growth.

### CONTEXTO
Estoy en la fase de conceptualización/investigación de un nuevo producto de software. Necesito que realices un análisis profundo, crítico y estratégico para validar y definir este producto.

### DATOS DE ENTRADA
* **Tipo de Producto:** [INSERTAR TIPO DE PRODUCTO AQUÍ, ej: CRM, App Móvil, SaaS B2B]
* **Problema Principal a Resolver:** [INSERTAR PROBLEMA, ej: La falta de comunicación en equipos remotos]
* **Público Objetivo (Target):** [INSERTAR PÚBLICO, ej: Startups de tecnología de 10 a 50 empleados]
* **Restricciones o Diferenciadores Clave:** [INSERTAR INFORMACIÓN EXTRA, ej: Debe usar IA, bajo presupuesto, enfoque en privacidad]

### INSTRUCCIONES ESTRATÉGICAS (PASO A PASO)
Debes desarrollar una respuesta estructurada que cubra las siguientes 5 dimensiones críticas del producto:

**1. Discovery & Validación del Problema (El "Por qué")**
* Analiza si el problema descrito es un "dolor real" (Pain Point) o solo una molestia.
* Utiliza el marco **Jobs to be Done (JTBD)** para definir qué intenta lograr realmente el usuario.
***2. Análisis de Mercado y Competencia**
* Identifica 3 competidores potenciales (directos o indirectos).
* Realiza un mini-análisis **SWOT (FODA)** enfocado en nuestra propuesta de valor frente a ellos.

**3. Definición de la Solución y User Personas**
* Crea un **User Persona** detallado (Nombre, Rol, Frustraciones, Metas).
* Define la **Propuesta de Valor Única (UVP)** en una sola frase potente.

**4. Roadmap de Funcionalidades (Priorización)**
* Propón las funcionalidades clave para el MVP (Producto Mínimo Viable).
* Utiliza el método **MoSCoW** (Must have, Should have, Could have, Won't have) para clasificar estas funciones.
* Justifica técnicamente por qué estas funciones son las primeras.

**5. Métricas de Éxito y KPIs**
* Define 3 **OKRs** (Objectives and Key Results) para los primeros 6 meses.
* Define las **North Star Metric** del producto.

### FORMATO DE SALIDA
Usa encabezados claros, tablas para la priorización y viñetas para facilitar la lectura. Mantén un tono profesional, objetivo y orientado a negocios.
```

### 💡 Definición y Refinamiento de Producto
Aquí están los prompts enfocados en la ideación, especificación de requisitos y evolución de las funcionalidades del software.

2. *"Quiero un producto Document Management Security (DMS)"*
3. *"Quiero que este producto aparte de seguridad tambien contenga control de versiones... APIs Rest... IA..."*

### 📝 Documentación, Estructura y Formato
Esta categoría incluye instrucciones para organizar la información generada, definir roles de salida específicos y solicitar formatos de archivo.

4. *"**Rol** Actúa como un analista experto... **Objetivo**... **Instrucciones**..."* (Prompt estructurado para la estructura del documento).
5. *"Dame esta informacion en markdown listo para copiar"*

## Fase de analisis de Arquitectura del Sistema
### 🧠 Ingeniería de Prompts y Metodología
1. *"Como experto en prompt enginner, generame un prompt para un experto en arquitectura de sistemas, especialmente en arquitectura microservicios. Donde detecte los servicios principales del producto. Debe solicitarme el analisis del producto y realice una breve descripcion de la arquitectura, lista de servicios (Microservicios). Aplica este prompt con las mejores practicas y solicitame todo lo necesario para tener completo el prompt."*

`Meta Prompt`
```
# ROLE
Actúa como un Arquitecto de Soluciones Senior y experto en Arquitectura de Microservicios con más de 15 años de experiencia. Tienes un profundo conocimiento en Domain-Driven Design (DDD), patrones de escalabilidad y sistemas distribuidos.

# CONTEXT
Estoy diseñando un nuevo producto digital (o refactorizando uno existente) y necesito descomponer la lógica de negocio en una arquitectura de microservicios desacoplada y escalable.

# INPUT DATA
Aquí tienes el análisis funcional del producto:
[Contenido Investigacion]

# TASKS
1.  **Análisis de Dominio:** Analiza la descripción del producto e identifica los "Bounded Contexts" (Contextos Delimitados) principales.
2.  **Diseño de Arquitectura:** Define una arquitectura de alto nivel adecuada para este producto.
3.  **Definición de Servicios:** Desglosa el sistema en microservicios específicos.

# OUTPUT FORMAT
Tu respuesta debe usar formato Markdown y seguir estrictamente esta estructura:

## 1. Resumen de la Arquitectura
Describe brevemente el estilo arquitectónico (ej. Event-Driven, Hexagonal, etc.) y por qué es ideal para este producto específico. Menciona los patrones de comunicación principales (REST, gRPC, Message Queues).

## 2. Identificación de Bounded Contexts
Explica brevemente cómo has agrupado las funcionalidades en dominios lógicos antes de separarlos en servicios.

## 3. Listado de Microservicios
Genera una tabla o lista detallada con los siguientes campos para CADA microservicio identificado:
* **Nombre del Servicio:** (Ej. `OrderService`)
* **Responsabilidad Principal:** Qué hace y qué NO hace.
* **Datos que maneja:** (Entidades principales, ej. Usuarios, Carrito, Inventario).
* **Dependencias:** Con qué otros servicios necesita comunicarse.
* **Justificación:** Por qué esto debe ser un microservicio separado y no parte de otro.

## 4. Diagrama Conceptual (Mermaid)
Genera el código para un diagrama de secuencia o diagrama de arquitectura usando sintaxis Mermaid que muestre la interacción crítica entre los 3 servicios más importantes.

# CONSTRAINTS
* Prioriza la alta cohesión y el bajo acoplamiento.
* Evita crear "nano-servicios" (servicios demasiado pequeños) o "monolitos distribuidos".
* Si detectas ambigüedad en el producto, asume el estándar de la industria para ese tipo de negocio pero anótalo como una suposición.
```

### 🔶 Meta-prompts y Prompt Engineering
**Prompts incluidos:**
- P1: “Como experto en prompt engineer, generame un meta prompt…”
- P2: “Agrega en este meta prompt el uso de tecnologías…”

`Meta prompt`
```
Organiza y desarrolla el contenido de forma clara, profesional y exhaustiva siguiendo los apartados indicados. Utiliza lenguaje técnico preciso y explica los conceptos de manera accesible pero rigurosa. Incluye siempre justificaciones arquitectónicas, beneficios, limitaciones, patrones, diagramas y ejemplos.

Este meta-prompt asume como stack base:

Frontend: React + TypeScript
Backend: Spring Boot (Java)
Bases de datos: MySQL y/o MongoDB (según el servicio)
Otros servicios: Añade aquellos que, según buenas prácticas, deberían existir (API Gateway, Load Balancer, Auth service, Cache, CI/CD, Observabilidad, etc.).

## Arquitectura del Sistema
### 1. Diagrama de Arquitectura
Incluye un diagrama Mermaid representando:

Frontend en React + TypeScript
Backend en Spring Boot (múltiples microservicios)
Bases de datos MySQL/MongoDB según el caso
Servicios adicionales por mejores prácticas (API Gateway, servicio de autenticación, CDN, cache Redis, message broker, etc.)
Infraestructura (contenedores, nube, redes, balanceadores…)
Explica si sigue un patrón como microservicios, arquitectura hexagonal, Clean Architecture, N-tier, etc.

Justifica por qué se eligió esta arquitectura.

Destaca beneficios clave y compromisos/sacrificios asociados.

### 2. Componentes Principales
Para cada componente del sistema, describe:

* Función y responsabilidades.
* Tecnología utilizada y justificación:
* React + TypeScript para frontend.
* Spring Boot para backend.
* MySQL para servicios transaccionales.
* MongoDB para servicios documentales o de alto volumen.
* Otros servicios sugeridos según buenas prácticas (cache, mensajería, gateway, monitorización…).
* Cómo interactúa con otros componentes.
* Patrones aplicados si corresponde (repositorio, controlador, DTO, servicios, etc.).

### 3. Descripción de Alto Nivel del Proyecto y Estructura de Ficheros
Proporciona una descripción resumida del proyecto.

Muestra un árbol de directorios (código) tanto para el frontend como para el backend:

* Estructura típica de React + TypeScript (src/components, hooks, context, services...).
* Estructura típica de Spring Boot (controllers, services, repositories, config…).
* Explica la función de cada carpeta principal.

Indica si obedece a un patrón específico como Clean Architecture, DDD, monorepo o multirepo.

4. Infraestructura y Despliegue
Describe la infraestructura del proyecto, incluyendo:

* Contenedores Docker
* Orquestación (Kubernetes o alternativa)
* API Gateway / Ingress
* Balanceadores de carga
* CDN para el frontend
* Sistemas de logs y monitorización (Prometheus, Grafana, ELK, etc.)
* Secret management (Vault, AWS Secrets Manager, etc.)
* Incluye un diagrama Mermaid.

### 5. Seguridad
Enumera y explica medidas de seguridad aplicadas:

* Control de acceso y autenticación (JWT, OAuth2, Keycloak, Auth0...).
* Sanitización y validación de datos.
* Uso de HTTPS y política de CORS.
* Gestión segura de secretos.
* Hardening de contenedores.
* Rules de firewall, VPC, IAM, RBAC en Kubernetes.
* Prevención de ataques comunes (SQLi, XSS, CSRF).
* Incluye ejemplos concretos cuando corresponda.

### 6. Tests
Resume los tipos de tests implementados:

* Frontend: unit tests (React Testing Library), e2e (Cypress).
* Backend: unit tests (JUnit), integración, contract tests.
* Infraestructura: tests de pipelines, escaneos de seguridad.
* Describe algunos casos relevantes.

Menciona herramientas utilizadas.

### Formato de Respuesta
* Usa Markdown correctamente estructurado.
* Incluye diagramas en Mermaid cuando sea apropiado.
* Añade tablas si aportan claridad.
* Asegura coherencia entre secciones.
* Si falta información, asume detalles razonables basados en buenas prácticas actuales.
```

### Prompt Diagrama Contexto
* **Los meta prompts con Gemini 3 PRO**
* **Uso de meta prompt con Grok Code Fast 1**
---
`Meta Prompt C1`
```
# ROL
Actúa como un Arquitecto de Soluciones Senior experto en modelado de software y documentación técnica utilizando el modelo C4.

# TAREA
Tu objetivo es analizar la descripción de un proyecto que te proporcionaré y generar exclusivamente el **Diagrama de Contexto (Nivel C1)** utilizando sintaxis **Mermaid.js**.

# REGLAS DE GENERACIÓN
1. **Alcance:** Genera SOLO el Nivel 1 (Contexto). No incluyas contenedores, componentes ni código.
2. **Entidades:** Debes identificar y distinguir claramente entre:
   - **Personas (Actors):** Usuarios que interactúan con el sistema.
   - **Sistema de Software (Focal Point):** El sistema que estamos diseñando (en el centro).
   - **Sistemas de Software Externos:** Otros sistemas con los que el sistema principal interactúa (APIs, bases de datos externas, servicios legacy).
3. **Relaciones:** Todas las flechas deben tener una etiqueta descriptiva que explique la interacción (ej: "Envía correos", "Consulta datos", "Autentica usuario").
4. **Sintaxis Mermaid:**
   - Usa `graph TD` o `flowchart TD`.
   - Usa formas simples pero distinguibles (ej: `((Actor))` para personas, `[Sistema]` para el foco, `[[Sistema Externo]]` para externos).
   - No uses estilos CSS complejos que puedan romper el renderizado; mantén la sintaxis limpia.

# FORMATO DE SALIDA
- Proporciona únicamente el bloque de código Mermaid.
- No añadas explicaciones previas ni posteriores fuera del bloque de código.

# INPUT DEL PROYECTO
Descripción del sistema:
"""
[AQUÍ PEGAS LA DESCRIPCIÓN DE TU PROYECTO]
"""
```

## Fase de Modelo de Datos
### 🧠 Ingeniería de Prompts y Metodología

`Meta prompt`
```
# ROL
Actúa como un Arquitecto de Base de Datos Senior y experto en modelado de datos con sintaxis Mermaid.js.

# CONTEXTO DEL PROYECTO
Estoy desarrollando un sistema de software con las siguientes características:
[INSERTA AQUÍ LA DESCRIPCIÓN DETALLADA DE TU PRODUCTO O REQUERIMIENTOS]

# TAREA
Tu objetivo es analizar los requerimientos anteriores y generar un código de diagrama Entidad-Relación (ER) utilizando Mermaid.js. El diagrama debe representar un modelo de base de datos relacional normalizado (preferiblemente en 3NF).

# REGLAS DE SINTAXIS Y DETALLE (ESTRICTO)
1. **Entidades:** Usa nombres en singular y en español.
2. **Atributos:** Debes incluir todos los atributos lógicos derivados del contexto.
   - Incluye el **tipo de dato** para cada atributo (ej. `int`, `varchar`, `datetime`, `boolean`).
   - Marca claramente la **Clave Primaria** con `PK`.
   - Marca claramente las **Claves Foráneas** con `FK`.
   - Añade comentarios entre comillas si el campo requiere explicación (ej. `string status "active/inactive"`).
3. **Relaciones:**
   - Define la cardinalidad exacta usando la notación "Crow's Foot" (patas de gallo):
     - `||--||` (Uno a uno)
     - `||--|{` (Uno a muchos obligatorio)
     - `||--o{` (Uno a muchos opcional)
     - `}|--|{` (Muchos a muchos - *Nota: Si encuentras una relación N:M, debes resolverla creando una tabla intermedia*).
   - Etiqueta la relación con un verbo descriptivo (ej. `: "places"`, `: "contains"`).

# FORMATO DE SALIDA
Proporciona únicamente el bloque de código Mermaid encapsulado para que pueda ser renderizado directamente.

Ejemplo de estructura esperada dentro del código:
erDiagram
    User {
        int id PK
        string correo
        int role_id FK
    }
    Role {
        int id PK
        string nombre
    }
    Role ||--o{ User : assigned_to

# PASO A PASO
1. Analiza el texto del proyecto para identificar las entidades principales.
2. Determina los atributos necesarios y sus tipos de datos.
3. Establece las relaciones y su cardinalidad lógica.
4. Genera el código Mermaid final.
```

```
# ROL
Actúa como un Auditor Líder de Normas ISO (especializado en ISO 27001, ISO 15489 e ISO 27701) y como Arquitecto de Base de Datos Senior. Tu objetivo es auditar la estructura de datos proporcionada para garantizar el cumplimiento normativo, la seguridad y la integridad de los datos.

# CONTEXTO
Estoy desarrollando un sistema de gestión (DMS) y necesito validar si mi esquema de base de datos (diagrama entidad-relación) cumple con los estándares internacionales requeridos para una certificación futura.

# TAREA
Analiza la estructura de datos que proporcionaré al final de este prompt (delimitada por "---") y realiza las siguientes acciones paso a paso:

1.  **Análisis de Integridad (ISO 15489 - Gestión Documental):**
    * Verifica si existen campos de metadatos críticos para el ciclo de vida (ej. fecha de creación, autor, versión, estado de retención).
    * Detecta la falta de trazabilidad (Audit Trails).

2.  **Análisis de Seguridad (ISO 27001 - Seguridad de la Información):**
    * Identifica datos sensibles que no parecen estar encriptados o protegidos.
    * Evalúa si el control de acceso (roles/permisos) está reflejado en la estructura.

3.  **Análisis de Privacidad (ISO 27701 / GDPR):**
    * Identifica PII (Información Personal Identificable).
    * Verifica si existen mecanismos para el "Derecho al Olvido" (ej. `soft_delete`, `consent_flags`).

4.  **Recomendaciones de Arquitectura:**
    * Sugiere campos faltantes obligatorios.
    * Sugiere cambios en los tipos de datos para mayor robustez.

# FORMATO DE SALIDA
Tu respuesta debe ser técnica, directa y estructurada de la siguiente manera:

## 1. Resumen Ejecutivo
Breve opinión sobre la madurez actual del esquema (Bajo/Medio/Alto).

## 2. Análisis de Brechas (Gap Analysis)
Usa una tabla con las siguientes columnas:
| Entidad/Campo | Norma Afectada | Riesgo Detectado | Sugerencia de Corrección |
| --- | --- | --- | --- |

## 3. Esquema Mejorado (Sugerencia)
Proporciona el diagrama corregido, añadiendo los campos faltantes (comenta los cambios con `// [ISO XXX] Motivo`).

---
[AQUÍ PEGA TU ESTRUCTURA DE DATOS: SQL, JSON, O LISTA DE CAMPOS]
---
```

```
# ROL
Actúa como un Arquitecto de Datos Senior y Experto en Modelado UML. Tienes un dominio profundo de la sintaxis Mermaid.js y de las mejores prácticas de diseño de bases de datos (normalización, integridad referencial y convenciones de nomenclatura).

# OBJETIVO
Tu tarea es tomar una descripción en lenguaje natural de entidades de un sistema, analizarla profundamente y generar dos salidas:
1. Un Diccionario de Datos técnico detallado.
2. Un diagrama Entidad-Relación (ERD) completo en código Mermaid.

# INSTRUCCIONES DE ANÁLISIS
Al recibir la descripción de las entidades, debes realizar lo siguiente:

1.  **Estandarización:** Si los nombres de los atributos son ambiguos, aplica `snake_case` para bases de datos SQL estándar.
2.  **Inferencia de Tipos:** Asigna el tipo de dato más apropiado (ej. `VARCHAR(255)`, `INT`, `UUID`, `BOOLEAN`, `DATETIME`) si no se especifica.
3.  **Identificación de Claves:**
    * Detecta o sugiere la Primary Key (PK) para cada entidad.
    * Detecta las Foreign Keys (FK) basándote en las relaciones descritas.
4.  **Restricciones:** Identifica explícitamente `NOT NULL`, `UNIQUE`, `DEFAULT`, etc.
5.  **Enriquecimiento (Best Practices):** Si el usuario no lo menciona, añade sugerencias de campos de auditoría estándar (ej. `created_at`, `updated_at`, `is_active`) para profesionalizar el diseño.

# FORMATO DE SALIDA 1: ANÁLISIS DE DATOS
Presenta una lista detallada por entidad con este formato:

**Nombre Entidad:** [Nombre]
* **Descripción:** [Breve propósito]
* **Atributos:**
    * `[nombre_campo]` | [Tipo] | [Constraints (PK, FK, Unique, Not Null)] | [Descripción breve]
* **Relaciones:** [Entidad A] se relaciona con [Entidad B] (Tipo: 1:1, 1:N, N:M).

# FORMATO DE SALIDA 2: CÓDIGO MERMAID
Genera un bloque de código `mermaid` utilizando la sintaxis `erDiagram`.
* Usa la notación correcta de cardinalidad: `||--o{`, `||--||`, `}|--|{`.
* Incluye el tipo de dato y restricciones dentro del diagrama si es posible.
* Asegura que las FK apunten correctamente a sus entidades padres.

---
[AQUÍ PEGARÁS LA DESCRIPCIÓN DE TUS ENTIDADES]
---
```

🏗️ Diseño y Modelado de Datos (Database Design)

Esta categoría agrupa las peticiones enfocadas en la creación, modificación y visualización de la estructura de la base de datos (Entidad-Relación).

    Prompts incluidos:
        "Dame una lista de entidades para crear una estructura de datos para un Document Manager System."
        "Analízame la siguiente E-R y modifica con las mejores prácticas..."
        "Modifícalo con este nuevo nombre y también corrige todos los campos a español... Dámelo en diagrama E-R Mermaid."
        "Dame el diagrama E-R en mermaid con los cambios a refactorizar."

📝 Documentación y Definición de Arquitectura

Prompts orientados a describir textualmente el sistema, explicar la lógica de negocio y generar entregables finales (Diccionario de Datos).

    Prompts incluidos:
        "Dame una descripción general de la estructura de base de datos como la funcionalidad de cada entidad y sus relaciones."
        "Descripción de entidades principales: Recuerda incluir el máximo detalle de cada entidad..."
        "Dame la descripción general de la arquitectura de datos... lo necesito en md para copiar la información."


🛠️ Validación Técnica y Mejores Prácticas

Consultas específicas sobre decisiones de ingeniería (SQL vs NoSQL) y justificación de atributos técnicos.

    Prompts incluidos:
        "Evalúa cada campo definido como JSON/JSONB y determina si está correctamente usado..."
        "Cuál es la finalidad del calcula su hash de cada versión documento."

## Especificación de la API
    "Como experto en prompt engineer, generame un prompt para un experto en producto y PM, que identifique un producto minimo viable MVP y me liste las funcionalidades principales del producto. "

`Meta prompt`
```
# ROL
Actúa como un Senior Product Manager y Estratega de Producto con experiencia en metodologías Lean Startup y Agile. Tienes un historial probado de lanzar productos digitales exitosos desde cero.

# CONTEXTO
Estoy desarrollando una nueva idea de producto:
[DESCRIBE TU IDEA DE PRODUCTO AQUÍ LO MÁS DETALLADO POSIBLE]

# TAREA
Tu objetivo es ayudarme a definir un Producto Mínimo Viable (MVP) sólido. No quiero solo una lista de deseos; necesito una hoja de ruta estratégica para validar mi hipótesis con el menor esfuerzo de desarrollo posible, pero entregando el máximo valor.

Por favor, genera lo siguiente:

1. **Hipótesis Central:** Define en una frase qué estamos intentando validar con este MVP.
2. **Funcionalidades del MVP (MoSCoW):** Utiliza el método MoSCoW (Must have, Should have, Could have, Won't have) para desglosar las funcionalidades.
   - Céntrate exclusivamente en las "Must have" (Imprescindibles) para el lanzamiento inicial.
   - Explica brevemente POR QUÉ cada funcionalidad es esencial para el MVP.
3. **User Journey del MVP:** Describe paso a paso el flujo simplificado que seguirá el usuario en esta versión reducida para resolver su problema.
4. **Métricas de Éxito (KPIs):** Sugiere 2 o 3 métricas clave que me indicarán si el MVP está funcionando.

# FORMATO DE SALIDA
Presenta la respuesta estructurada, utilizando tablas para la sección de funcionalidades y listas con viñetas para el resto. Mantén un tono profesional, analítico y orientado a la acción.
```

## Historias de usuario
    "Como experto prompt engineer, generame un prompt donde solicite el contexto de un MVP e identifique las epicas del proyecto para despues proceder generar historias de usuarios de cada epica."

```
# ROL
Actúa como un Product Owner Senior y experto en metodologías Ágiles (Scrum/Kanban). Eres experto en desglosar visiones de producto en entregables técnicos y funcionales, utilizando el criterio INVEST para historias de usuario.

# OBJETIVO
Tu objetivo es guiarme en el proceso de definición de mi MVP. No generarás todo el contenido de golpe. Seguiremos un flujo de trabajo iterativo paso a paso.

# INSTRUCCIONES DE FLUJO DE TRABAJO

### FASE 1: ENTENDIMIENTO (Estado actual)
Si yo no te he proporcionado la descripción de mi producto todavía, TU PRIMERA RESPUESTA debe solicitarme archivo para entender el contexto.

> ESPERA MI RESPUESTA A ESTAS PREGUNTAS ANTES DE PASAR A LA FASE 2.

### FASE 2: DEFINICIÓN DE ÉPICAS
Una vez tengas el contexto:
1. Analiza la información y propón una lista de **Épicas** principales para el MVP.
2. Cada Épica debe tener:
   - Título claro.
   - Breve descripción del alcance.
3. Al final de la lista, pregúntame: *"¿Qué Épica te gustaría priorizar para desglosar en Historias de Usuario ahora?"*

### FASE 3: GENERACIÓN DE HISTORIAS DE USUARIO (Bucle)
Cuando yo seleccione una Épica:
1. Genera las Historias de Usuario asociadas a esa Épica específica.
2. Usa estrictamente el siguiente formato para cada historia:
   
   **[ID] Título de la Historia**
   - **Narrativa:** Como [rol], quiero [acción], para que [beneficio].
   - **Criterios de Aceptación (Gherkin):**
     - *Scenario 1:* Dado [contexto], Cuando [evento], Entonces [resultado].
   - **Notas Técnicas/Datos:** (Opcional, si aplica).

3. Al terminar, pregúntame si quiero refinar esas historias o pasar a la siguiente Épica.

# REGLA FINAL
Mantén un tono profesional, estructurado y enfocado en maximizar el valor del MVP. Empieza ahora con la FASE 1.
```

## Tickets
`Meta prompt`
```
A partir de la siguiente Historia de Usuario, genera una lista completa de tickets necesarios para implementar la funcionalidad desde inicio a fin.

Historia de Usuario:
[Pegar historia de usuario aquí]

Tu tarea:
– Analizar la historia.
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

Reglas del prompt:
– No agregar funcionalidades nuevas que no existan en la historia.
– Si la historia es muy grande, divídela en varias historias y luego en tickets.
– Usa lenguaje claro para producto, diseño, desarrollo y QA.
– Asegura que cada ticket sea independiente, estimable y verificable.
– Prioriza granularidad útil: ni demasiado grande ni demasiado pequeña.
- Dividir los tickets en Base de datos, Backend y Frontend.
```


