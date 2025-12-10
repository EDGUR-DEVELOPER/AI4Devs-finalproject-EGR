`Prompt para organizar el archivo prompts.md`
```
Analiza todos los prompts generados en esta sesión y clasifícalos en categorías temáticas. Para cada categoría, indica:- Nombre de la categoría- Lista de prompts incluidos (con IDs o títulos si hay)- Cantidad total de prompts por categoría
 Luego, responde:- ¿Cuál es la categoría con más prompts?- ¿Qué áreas necesitan más prompts para balancear?
 Presenta todo en un bloque claro de Markdown, con emojis y formato de lista.
```
---
***Estos prompt se ejecutaron con Gemini 3 PRO***
## Fase de analisis del producto
### 🧠 Categoría 1: Ingeniería de Prompts y Metodología
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

### 💡 Categoría 2: Definición y Refinamiento de Producto
Aquí están los prompts enfocados en la ideación, especificación de requisitos y evolución de las funcionalidades del software.

2. *"Quiero un producto Document Management Security (DMS)"*
3. *"Quiero que este producto aparte de seguridad tambien contenga control de versiones... APIs Rest... IA..."*

### 📝 Categoría 3: Documentación, Estructura y Formato
Esta categoría incluye instrucciones para organizar la información generada, definir roles de salida específicos y solicitar formatos de archivo.

4. *"**Rol** Actúa como un analista experto... **Objetivo**... **Instrucciones**..."* (Prompt estructurado para la estructura del documento).
5. *"Dame esta informacion en markdown listo para copiar"*

## Fase de analisis de Arquitectura del Sistema
### 🧠 Categoría 1: Ingeniería de Prompts y Metodología
1. *"Como experto en prompt enginner, generame un prompt para un experto en arquitectura de sistemas, especialmente en arquitectura microservicios. Donde detecte los servicios principales del producto. Debe solicitarme el analisis del producto y realice una breve descripcion de la arquitectura, lista de servicios (Microservicios). Aplica este prompt con las mejores practicas y solicitame todo lo necesario para tener completo el prompt."*

`Meta Prompt`
```
**Rol:**
Actúa como un **Arquitecto de Soluciones Senior** (Senior Solution Architect) especializado en sistemas distribuidos, patrones de diseño de microservicios y *Domain-Driven Design (DDD)*. Tu enfoque debe priorizar la escalabilidad, la tolerancia a fallos y el bajo acoplamiento.

**Contexto:**
Estoy en la fase de diseño técnico de un nuevo producto digital y necesito definir los "Bounded Contexts" (Contextos Delimitados) para establecer una arquitectura de microservicios sólida.

**Tu Tarea:**
Analiza la información del producto proporcionada al final de este prompt. Basándote en ese análisis, genera una propuesta de arquitectura técnica siguiendo estrictamente estos pasos:

1.  **Análisis del Dominio y Entidades:**
    * Identifica los dominios principales del negocio.
    * Lista las entidades de datos críticas (Core Entities).

2.  **Estilo Arquitectónico:**
    * Describe brevemente el estilo arquitectónico recomendado (ej. Event-Driven, Hexagonal, Layered, etc.) y justifica por qué es el adecuado para este caso de uso específico.

3.  **Definición de Microservicios (El Núcleo):**
    * Genera una lista detallada de los microservicios necesarios. Para cada uno, utiliza el siguiente formato:
        * **Nombre del Servicio:** (Debe ser representativo del dominio).
        * **Responsabilidad Única:** ¿Qué función de negocio resuelve exclusivamente este servicio?
        * **Datos/Entidades:** ¿De qué datos es "dueño" (System of Record)?
        * **Comunicación:** ¿Qué eventos publica y a qué otros servicios necesita llamar (Síncrono vs Asíncrono)?

4.  **Diagrama de Flujo (Mermaid):**
    * Genera un bloque de código con sintaxis `mermaid` (graph TD) que visualice las relaciones y dependencias entre estos servicios y bases de datos.

---

**Información del Producto (INPUT):**

* **Nombre del Producto:** [INSERTA AQUÍ EL NOMBRE]
* **Descripción del Problema/Solución:** [DESCRIBE AQUÍ QUÉ HACE EL PRODUCTO, QUÉ PROBLEMA RESUELVE Y CÓMO FUNCIONA A GRANDES RASGOS]
* **Usuarios Principales:** [LISTA LOS TIPOS DE USUARIOS: EJ. ADMIN, CLIENTE FINAL, PROVEEDOR]
* **Funcionalidades Clave (Core Features):**
    * [FEATURE 1: Ej. Registro de usuarios y login social]
    * [FEATURE 2: Ej. Carrito de compras y checkout]
    * [FEATURE 3: Ej. Procesamiento de pagos recurrentes]
    * [FEATURE 4: Ej. Generación de reportes en PDF]
* **Requisitos No Funcionales Críticos:** [EJ. ALTA DISPONIBILIDAD, BAJA LATENCIA, SEGURIDAD HIPAA/GDPR, SOPORTE PARA 100K USUARIOS CONCURRENTES]
* **Integraciones Externas:** [EJ. STRIPE, GOOGLE MAPS, SAP]

---

**Formato de Salida:**
Responde en formato Markdown bien estructurado. Sé técnico, crítico y profesional.
```