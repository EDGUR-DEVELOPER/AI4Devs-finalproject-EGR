# Identity Service

IAM Service - Lightweight Keycloak Wrapper for DocFlow.

## Stack

- **Java**: 21
- **Spring Boot**: 3.5.0
- **Build Tool**: Maven
- **Packaging**: JAR ejecutable

## Arquitectura

Este proyecto sigue una **Arquitectura Hexagonal** (Ports & Adapters):

```
src/main/java/com/docflow/identity/
├── IdentityApplication.java          # Clase principal
├── application/                       # Capa de Aplicación
│   ├── dto/                          # Data Transfer Objects
│   ├── ports/
│   │   ├── input/                    # Puertos de entrada (casos de uso)
│   │   └── output/                   # Puertos de salida (repositorios)
│   └── services/                     # Implementación de casos de uso
├── domain/                           # Capa de Dominio (lógica pura)
│   ├── exceptions/                   # Excepciones de negocio
│   ├── model/                        # Entidades y Value Objects
│   └── service/                      # Servicios de dominio
└── infrastructure/                   # Capa de Infraestructura
    ├── adapters/
    │   ├── input/
    │   │   └── rest/                 # Controladores REST
    │   │       └── HelloController.java
    │   └── output/
    │       └── persistence/          # Implementación JPA
    └── config/                       # Configuración de Spring
```

## Dependencias

| Dependencia | Propósito |
|-------------|-----------|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | Persistencia (preparado) |
| `spring-boot-starter-validation` | Validación de datos |
| `spring-boot-starter-test` | Testing con JUnit 5 |
| `mapstruct` | Mapeo de objetos |
| `springdoc-openapi` | Documentación API (Swagger) |
| `lombok` | Reducción de boilerplate |

## Requisitos Previos

- **Java 21** instalado y configurado en `JAVA_HOME`
- **Maven 3.9+** (o usar el wrapper `./mvnw`)

Verificar instalación:
```bash
java -version   # Debe mostrar Java 21
mvn -version    # Debe mostrar Maven 3.9+
```

## Compilar

```bash
cd backend/identity

# Compilar el proyecto
mvn clean compile

# Compilar y empaquetar (genera JAR ejecutable)
mvn clean package

# Compilar sin ejecutar tests
mvn clean package -DskipTests
```

## Ejecutar Tests

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests con detalle
mvn test -Dtest=IdentityApplicationTests
```

## Ejecutar la Aplicación

### Opción 1: Con Maven (desarrollo)

```bash
mvn spring-boot:run
```

### Opción 2: Con JAR ejecutable (producción)

```bash
# Primero compilar
mvn clean package

# Luego ejecutar
java -jar target/identity-service-0.0.1-SNAPSHOT.jar
```

## Verificar Funcionamiento

Una vez iniciada la aplicación:

| Recurso | URL |
|---------|-----|
| **Hello Endpoint** | http://localhost:8081/api/v1/hello |
| **Swagger UI** | http://localhost:8081/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8081/api-docs |

### Probar con cURL

```bash
curl http://localhost:8081/api/v1/hello
```

Respuesta esperada:
```json
{
  "message": "Hello from Identity Service!",
  "service": "identity-service",
  "version": "0.0.1-SNAPSHOT",
  "timestamp": "2025-12-31T12:00:00.000000",
  "status": "UP"
}
```

## Estructura de Directorios Completa

```
identity/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── docflow/
    │   │           └── identity/
    │   │               ├── IdentityApplication.java
    │   │               ├── application/
    │   │               │   ├── dto/
    │   │               │   ├── ports/
    │   │               │   │   ├── input/
    │   │               │   │   └── output/
    │   │               │   └── services/
    │   │               ├── domain/
    │   │               │   ├── exceptions/
    │   │               │   ├── model/
    │   │               │   └── service/
    │   │               └── infrastructure/
    │   │                   ├── adapters/
    │   │                   │   ├── input/
    │   │                   │   │   └── rest/
    │   │                   │   │       └── HelloController.java
    │   │                   │   └── output/
    │   │                   │       └── persistence/
    │   │                   └── config/
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
            └── com/
                └── docflow/
                    └── identity/
                        └── IdentityApplicationTests.java
```

## Configuración (application.yml)

```yaml
spring:
  application:
    name: identity-service

server:
  port: 8081

springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

## Próximos Pasos

1. Configurar base de datos PostgreSQL
2. Integrar Keycloak como proveedor de identidad
3. Implementar endpoints de autenticación (`/auth/login`, `/auth/switch`)
4. Agregar Spring Security con JWT

## Licencia

Proyecto interno - DocFlow DMS
