# Instrucciones rápidas para agentes (Copilot) — DocFlow

Breve y accionable: estas notas ayudan a un agente a ser productivo inmediatamente en este repo.

- **Arquitectura (alto nivel):** proyecto modular con frontend SPA (`frontend/`, React+Vite) y varios microservicios Java Spring Boot en `backend/` (ej. `document-core`, `gateway`, `identity`). La infraestructura local está orquestada por `docker-compose.yml` y descrita en `README-docker.md`.

- **Puntos clave de diseño:**
  - Backend sigue un estilo Hexagonal / Ports & Adapters (ver `README.md` secciones "Hexagonal").
  - Frontend organiza por *features* (`src/features/*`) — buscar `services`, `hooks`, `types` dentro de cada feature.

- **Entorno local rápido:**
  1. Copiar variables: `cp .env.example .env` (ajustar si hace falta).
  2. Levantar infra: `docker compose up -d` (usa `docker-compose.yml`). Consulte `README-docker.md` para puertos y credenciales (MinIO: `docflow-documents`, Postgres: `docflow`/`docflow_secret`).
  3. Frontend: `cd frontend && npm install && npm run dev` (Vite en `http://localhost:5173`).
  4. Backend (por servicio): ir a `backend/<service>` y ejecutar `mvn spring-boot:run` (ej.: `cd backend/gateway && mvn spring-boot:run`).

- **Comandos de build/test relevantes:**
  - Frontend build: `cd frontend && npm run build` (usa `vite` + `tsc -b`).
  - Backend (por módulo): `cd backend/<module> && mvn clean package` o `mvn test` para tests unitarios. Los informes de test aparecen en `target/surefire-reports`.

- **Convenciones detectadas en el repo:**
  - `backend/*`: cada servicio es un proyecto Spring Boot independiente (usa `spring-boot-maven-plugin`, Java 21). MapStruct y Lombok están presentes (ver `pom.xml`).
  - CORS se configura vía `CORS_ALLOWED_ORIGINS` y no permite `*` (ver `README-docker.md` sección CORS).
  - Objetos binarios se almacenan en S3/MinIO con bucket por defecto `docflow-documents` (ver `docker-compose.yml` y `README-docker.md`).

- **Patrones y archivos a revisar para cambios concretos:**
  - APIs y contratos: `backend/*` usan `springdoc-openapi` (Swagger UI disponible en cada servicio cuando corre).
  - Adaptadores/infrastructure: buscar `infrastructure/adapters` dentro de servicios para puntos de integración (storage, broker, persistence).
  - Frontend -> `src/features/*/services/*.ts` para llamadas axios y manejo de tokens.

- **Integraciones y dependencias externas críticas:**
  - Postgres (5432), MinIO (9000/9001) — orquestadas opcionalmente por `docker compose`.
  - JWT handling en gateway (`io.jsonwebtoken`), y `spring-cloud-gateway` para enrutamiento.

- **Ejemplos concretos para referencia rápida:**
  - Levantar infraestructura: `docker compose up -d` (archivo: `docker-compose.yml`).
  - Iniciar gateway: `cd backend/gateway && mvn spring-boot:run` (pom: `backend/gateway/pom.xml`).
  - Revisar front: `frontend/package.json` (`npm run dev`, `npm run build`).

- **Qué evitar/considerar para PRs de código:**
  - No cambiar CORS a `*` ni exponer credenciales en código.
  - Mantener separaciones: no mezclar lógica de dominio con adaptadores (seguir estructura `domain` / `application` / `infrastructure`).

- **Reglas de desarrollo del proyecto:**
  
  Este documento es el índice general de reglas para el proyecto **DocFlow**.
  
  - Para cambios en servicios **backend** revisa: [rules-backend.md](./rules-backend.md)
  - Para cambios en la **aplicación frontend** revisa: [rules-frontend.md](./rules-frontend.md)
  - Para cambios en **base de datos** revisa: [rules-database.md](./rules-database.md)
  - Para cambios de **infraestructura y Docker** revisa: [rules-infra-docker.md](./rules-infra-docker.md)
  
- **Cómo usar este índice:**
  
  1. Identifica qué parte del sistema tocas en tu cambio (backend, frontend, BD, infra).
  2. Lee el documento de reglas específico para esa área.
  3. Asegúrate de cumplir también con las reglas de **pruebas** (TDD, unitarias, integración) cuando agregues o modifiques funcionalidad.
  4. Describe en tu Pull Request cómo aplicaste estas reglas.
  
  Las reglas pueden evolucionar; cualquier cambio importante debe ser conversado con el equipo y reflejado aquí y en los prompts originales cuando aplique.
