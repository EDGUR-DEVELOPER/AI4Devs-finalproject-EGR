# Audit Log Service

Reactive microservice for audit event management in DocFlow.

## 📋 Prerequisites

- **Java 21** (OpenJDK or Oracle JDK)
- **Maven 3.9+**

## 🏗️ Architecture

This service follows **Hexagonal Architecture** (Ports & Adapters) pattern:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Infrastructure                           │
│  ┌─────────────────┐                    ┌─────────────────────┐ │
│  │   REST API      │                    │    MongoDB          │ │
│  │   (WebFlux)     │                    │    (Reactive)       │ │
│  └────────┬────────┘                    └──────────┬──────────┘ │
│           │                                        │            │
│  ┌────────▼────────┐                    ┌──────────▼──────────┐ │
│  │  Input Adapter  │                    │   Output Adapter    │ │
│  └────────┬────────┘                    └──────────┬──────────┘ │
├───────────┼────────────────────────────────────────┼────────────┤
│           │           Application                  │            │
│  ┌────────▼────────┐                    ┌──────────▼──────────┐ │
│  │   Input Port    │◄──────────────────►│    Output Port      │ │
│  │   (Use Cases)   │                    │   (Repositories)    │ │
│  └────────┬────────┘                    └──────────┬──────────┘ │
│           │                                        │            │
│  ┌────────▼────────────────────────────────────────▼──────────┐ │
│  │                      Services                              │ │
│  └────────────────────────────┬───────────────────────────────┘ │
├───────────────────────────────┼─────────────────────────────────┤
│                               │                                 │
│  ┌────────────────────────────▼───────────────────────────────┐ │
│  │                       Domain                               │ │
│  │              (Entities, Value Objects, Services)           │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 📦 Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-webflux` | Reactive REST API with WebFlux |
| `spring-boot-starter-validation` | Bean validation |
| `spring-boot-starter-data-mongodb-reactive` | Reactive MongoDB (prepared) |
| `springdoc-openapi-starter-webflux-ui` | OpenAPI/Swagger documentation |
| `lombok` | Boilerplate reduction |
| `spring-boot-starter-test` | Testing with JUnit 5 |
| `reactor-test` | Reactive testing utilities |

## 🔧 Build

```bash
cd backend/auditLog
mvn clean package
```

## 🧪 Test

```bash
mvn test
```

## 🚀 Run

### Using Maven

```bash
mvn spring-boot:run
```

### Using JAR

```bash
java -jar target/auditlog-service-0.0.1-SNAPSHOT.jar
```

## 🌐 Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | http://localhost:8083/health | Health check endpoint |
| GET | http://localhost:8083/swagger-ui.html | Swagger UI |
| GET | http://localhost:8083/api-docs | OpenAPI JSON specification |

## ⚙️ Configuration

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | `8083` | HTTP server port |
| `spring.application.name` | `auditlog-service` | Application name |

## 📁 Directory Structure

```
auditLog/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/docflow/audit/
    │   │   ├── AuditLogApplication.java
    │   │   ├── application/
    │   │   │   ├── dto/
    │   │   │   ├── ports/
    │   │   │   │   ├── input/
    │   │   │   │   └── output/
    │   │   │   └── services/
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   ├── exceptions/
    │   │   │   └── service/
    │   │   └── infrastructure/
    │   │       ├── adapters/
    │   │       │   ├── input/rest/
    │   │       │   │   └── HealthController.java
    │   │       │   └── output/persistence/
    │   │       └── config/
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/docflow/audit/
            ├── AuditLogApplicationTests.java
            └── infrastructure/adapters/input/rest/
                └── HealthControllerTest.java
```

## 🔜 Next Steps

- [ ] Configure MongoDB connection
- [ ] Implement audit event domain model
- [ ] Create audit event emission service
- [ ] Add query endpoints with pagination
- [ ] Implement security integration

## 📝 Stack

- **Java**: 21
- **Spring Boot**: 3.5.0
- **Web Framework**: WebFlux (Reactive)
- **Database**: MongoDB Reactive (prepared)
- **API Documentation**: SpringDoc OpenAPI 2.7.0
- **Build Tool**: Maven
- **Packaging**: JAR (executable)
