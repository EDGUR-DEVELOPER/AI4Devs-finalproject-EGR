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

### List Folder Content with Permission-Based Visibility

Endpoints to list folder contents (subfolders and documents) with user-specific access filtering (US-FOLDER-002).

- **List folder content**: `GET /api/carpetas/{carpetaId}/contenido`
  - Parameters: `pagina` (default: 1), `tamanio` (1-100, default: 20), `campoOrden` (default: "nombre"), `direccion` (ASC/DESC, default: ASC)
  - Headers: `X-User-Id` (required), `X-Organization-Id` (required)
  - Response: Paginated list of accessible subfolders and documents with user capabilities

- **List root folder content**: `GET /api/carpetas/raiz/contenido`
  - Parameters: `pagina` (default: 1), `tamanio` (1-100, default: 20), `campoOrden` (default: "nombre"), `direccion` (ASC/DESC, default: ASC)
  - Headers: `X-User-Id` (required), `X-Organization-Id` (required)
  - Response: Paginated list of root folder contents filtered by user permissions
- **Update permission**: `PATCH /api/carpetas/{carpetaId}/permisos/{usuarioId}`
- **Revoke permission**: `DELETE /api/carpetas/{carpetaId}/permisos/{usuarioId}`

Revocation performs a hard delete of the permission entry and is intended for administrators.

### Document Versioning (US-DOC-003)

The service supports complete document version history with immutable version storage.

- **Create new version**: `POST /api/documentos/{id}/versiones`
  - Content-Type: `multipart/form-data`
  - Form fields:
    - `file` (required): The file to upload as the new version (max 500 MB)
    - `comentarioCambio` (optional): Description of changes in this version (max 500 characters)
  - Headers: `X-User-Id` (required), `X-Organization-Id` (required)
  - Requirements: User must have ESCRITURA (write) permission on the document
  - Response: Version details including sequential version number and metadata
  - HTTP Status:
    - `201 Created`: Version created successfully
    - `400 Bad Request`: Invalid file (empty, too large, etc.)
    - `403 Forbidden`: User lacks ESCRITURA permission
    - `404 Not Found`: Document not found
    - `409 Conflict`: Concurrent version upload detected
    - `413 Payload Too Large`: File exceeds 500 MB

**Version Management Features:**
- Automatic sequential version numbering (1, 2, 3, ...)
- Immutable version history (versions cannot be modified or deleted)
- SHA256 hash calculation for content integrity
- Current version tracking (`version_actual_id` updated automatically)
- Document metadata updated with each new version (size, modification date)

Response example:

```json
{
    "id": 201,
    "documentoId": 100,
    "numeroSecuencial": 2,
    "tamanioBytes": 2048576,
    "hashContenido": "a3c7f9e2b1d4c5a6e8f9b2d3c4e5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3",
    "comentarioCambio": "Actualización Q1 2026",
    "creadoPor": 1,
    "fechaCreacion": "2026-02-09T14:30:00Z",
    "esVersionActual": true
}
```

### Document Version History (US-DOC-004)

The service exposes an endpoint to list the full version history of a document, ordered by sequential version number.

- **List document versions**: `GET /api/documentos/{documentoId}/versiones`
  - Query params: `pagina` (base 1, optional), `tamanio` (1-100, default 20)
  - Headers: `X-User-Id` (required), `X-Organization-Id` (required)
  - Requirements: User must have LECTURA (read) permission on the document
  - Response: List of versions with optional pagination metadata

Response example:

```json
{
  "versiones": [
    {
      "id": 200,
      "numeroSecuencial": 1,
      "tamanioBytes": 1024,
      "hashContenido": "a3c7f9e2b1d4c5a6e8f9b2d3c4e5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3",
      "comentarioCambio": "Inicial",
      "creadoPor": {
        "id": 1,
        "nombreCompleto": "Usuario Test",
        "email": "test@docflow.com"
      },
      "fechaCreacion": "2026-02-09T14:30:00Z",
      "descargas": 2,
      "ultimaDescargaEn": "2026-02-10T10:00:00Z",
      "esVersionActual": false
    }
  ],
  "documentoId": 100,
  "totalVersiones": 1,
  "paginacion": {
    "paginaActual": 1,
    "tamanio": 20,
    "totalPaginas": 1,
    "totalElementos": 1,
    "primeraPagina": true,
    "ultimaPagina": true
  }
}
```

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
