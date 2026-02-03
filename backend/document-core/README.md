# Document Core Service

Document Core Service for DocFlow - Microservice for document management.

## Prerequisites

- **Java 21** (JDK 21+)
- **Maven 3.9+**

## Tech Stack

- Java 21
- Spring Boot 3.5.0
- Spring Data JPA (prepared, not configured)
- MapStruct 1.5.5.Final
- Lombok
- SpringDoc OpenAPI 2.7.0

## Project Structure

```
src/main/java/com/docflow/documentcore/
├── DocumentCoreApplication.java     # Main entry point
├── HelloController.java             # Health check endpoint
├── application/                     # Application Layer
│   ├── dto/                        # Data Transfer Objects
│   ├── ports/
│   └── services/                   # Use case implementations
├── domain/                         # Domain Layer
│   ├── exceptions/                 # Business exceptions
│   ├── model/                      # Entities and Value Objects
│   └── service/                    # Domain services
└── infrastructure/                 # Infrastructure Layer
    ├── adapters/
    └── config/                    # Spring configuration
```

## Build

Compile and package the application:

```bash
mvn clean install
```

## Test

Run all tests:

```bash
mvn test
```

## Run

Start the application locally:

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/document-core-service-0.0.1-SNAPSHOT.jar
```

## Endpoints

Once running, the service is available at:

- **Base URL:** http://localhost:8082
- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **OpenAPI Docs:** http://localhost:8082/api-docs

### Folder ACL Permissions

The Document Core service exposes endpoints to manage explicit folder permissions (ACL) for users.

- **Create permission**: `POST /api/carpetas/{carpetaId}/permisos`
- **Update permission**: `PATCH /api/carpetas/{carpetaId}/permisos/{usuarioId}`
- **Revoke permission**: `DELETE /api/carpetas/{carpetaId}/permisos/{usuarioId}`

Revocation performs a hard delete of the permission entry and is intended for administrators.

### Effective Folder Permissions (US-ACL-004)

The service also exposes an endpoint to resolve the effective permission for the authenticated user,
including inherited permissions from ancestor folders when `recursivo=true`.

- **Get effective permission**: `GET /api/carpetas/{carpetaId}/mi-permiso`

Response example:

```json
{
    "nivel_acceso": "LECTURA",
    "es_heredado": true,
    "carpeta_origen_id": 2,
    "carpeta_origen_nombre": "Proyectos",
    "ruta_herencia": ["Proyectos", "2024", "Q1"]
}
```

## Configuration

Configuration is in `src/main/resources/application.yml`:

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | 8082 | HTTP port |
| `spring.application.name` | document-core-service | Service name |

## Notes

- Database configuration is not included yet. DataSource auto-configuration is excluded.
- Security is not configured yet. Prepared for future integration.

## Development rules

For general backend development conventions (architecture, testing, naming) see:

- [.github/rules-backend.md](../../.github/rules-backend.md)
- Project rules index: [.github/RULES.md](../../.github/RULES.md)
