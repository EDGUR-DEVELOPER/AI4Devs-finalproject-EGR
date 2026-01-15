# DocFlow Gateway Service

API Gateway for DocFlow microservices architecture. This service acts as the single entry point for all client requests, routing them to the appropriate backend microservices.

## Prerequisites

- **Java**: 21 (LTS)
- **Maven**: 3.9+

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.4.2 | Framework |
| Spring Cloud Gateway | 2024.0.0 | API Gateway |
| Spring WebFlux | 3.4.2 | Reactive Web |
| SpringDoc OpenAPI | 2.7.0 | API Documentation |
| Lombok | Latest | Boilerplate Reduction |

## Project Structure

```
gateway/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/docflow/gateway/
    │   │   ├── GatewayApplication.java
    │   │   └── infrastructure/
    │   │       ├── adapters/input/rest/
    │   │       │   └── HealthController.java
    │   │       └── config/
    │   │           ├── GatewayConfig.java
    │   │           └── GlobalHeaderFilter.java
    │   └── resources/
    │       └── application.yml
    └── test/java/com/docflow/gateway/
        ├── GatewayApplicationTests.java
        └── infrastructure/
            ├── adapters/input/rest/
            │   └── HealthControllerTest.java
            └── config/
                ├── GatewayConfigTest.java
                └── GlobalHeaderFilterTest.java
```

## Build

Compile and package the application:

```bash
mvn clean package
```

Build without running tests:

```bash
mvn clean package -DskipTests
```

## Test

Run all unit tests:

```bash
mvn test
```

Run tests with verbose output:

```bash
mvn test -X
```

## Run

### Using Maven

```bash
mvn spring-boot:run
```

### Using JAR

```bash
java -jar target/gateway-service-0.0.1-SNAPSHOT.jar
```

The service will start on **port 8080**.

## Endpoints

### Gateway Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `http://localhost:8080/health` | Health check |
| GET | `http://localhost:8080/webjars/swagger-ui/index.html` | Swagger UI |
| GET | `http://localhost:8080/v3/api-docs` | OpenAPI JSON |

### Routed Endpoints (MVP)

| Pattern | Target Service | Port |
|---------|----------------|------|
| `/api/iam/**` | Identity Service | 8081 |
| `/api/doc/**` | Document Core Service | 8082 |

**Nota:** Todas las rutas usan `stripPrefix(2)`, significando:
- `/api/iam/users` → `http://localhost:8081/users`
- `/api/doc/documents` → `http://localhost:8082/documents`

## Testing the Health Endpoint

Using cURL:

```bash
curl -i http://localhost:8080/health
```

Expected response:

```
HTTP/1.1 200 OK
Content-Type: application/json
X-DocFlow-Gateway: v1

{"status":"ok"}
```

Using PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/health
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8080 | Gateway server port |
| `spring.application.name` | gateway-service | Application name |

## Port Mapping (MVP)

| Service | Port |
|---------|------|
| **Gateway** | **8080** |
| Identity Service | 8081 |
| Document Core Service | 8082 |
| MinIO API | 9000 |
| MinIO Console | 9001 |
| PostgreSQL | 5432 |

**Nota MVP:** Redis, MongoDB, Kafka, Vault y sus puertos asociados han sido eliminados de la infraestructura para acelerar la entrega. Se implementarán en fases posteriores.

## Global Response Headers

All responses passing through the gateway include:

| Header | Value |
|--------|-------|
| `X-DocFlow-Gateway` | `v1` |

## License

Copyright © 2025 DocFlow Team
