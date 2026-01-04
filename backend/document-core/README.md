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
│   │   ├── input/                  # Input ports (use cases)
│   │   └── output/                 # Output ports (repositories)
│   └── services/                   # Use case implementations
├── domain/                         # Domain Layer
│   ├── exceptions/                 # Business exceptions
│   ├── model/                      # Entities and Value Objects
│   └── service/                    # Domain services
└── infrastructure/                 # Infrastructure Layer
    ├── adapters/
    │   ├── input/rest/            # REST Controllers
    │   └── output/persistence/    # JPA implementations
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
- **Hello Endpoint:** http://localhost:8082/hello
- **Swagger UI:** http://localhost:8082/swagger-ui.html
- **OpenAPI Docs:** http://localhost:8082/api-docs

## Verify Installation

After starting the service, verify it's running:

```bash
curl http://localhost:8082/hello
```

Expected response:

```json
{"message": "Hello Document Core"}
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
