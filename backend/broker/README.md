# Message Broker Service

Servicio de integración con Apache Kafka para DocFlow. Proporciona endpoints REST para publicar mensajes a topics de Kafka y un consumidor demo para pruebas.

## 📋 Descripción

Este microservicio actúa como puente entre los servicios de DocFlow y Apache Kafka, permitiendo:

- **Publicación de mensajes** vía REST API con metadata de respuesta
- **Consumo de mensajes** con listener demo configurable
- **Documentación OpenAPI** integrada con Swagger UI

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    Message Broker Service                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │ HealthController│    │ PublishController│                    │
│  │   GET /health   │    │  POST /publish   │                    │
│  └─────────────────┘    └────────┬────────┘                     │
│                                  │                               │
│                         ┌────────▼────────┐                     │
│                         │  KafkaTemplate  │                     │
│                         └────────┬────────┘                     │
├──────────────────────────────────┼──────────────────────────────┤
│                                  │                               │
│  ┌───────────────────────────────▼───────────────────────────┐  │
│  │                    Apache Kafka (KRaft)                    │  │
│  │                                                            │  │
│  │   ┌──────────────┐                                        │  │
│  │   │  demo-topic  │◄──────── DemoListener                  │  │
│  │   └──────────────┘                                        │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 🛠️ Stack Tecnológico

| Componente | Versión |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.5.0 |
| Spring Kafka | 3.3.x (gestionado por Spring Boot) |
| SpringDoc OpenAPI | 2.7.0 |
| Apache Kafka | 3.7.0 (KRaft mode) |
| Maven | 3.9+ |

## 📦 Dependencias

| Dependencia | Propósito |
|-------------|-----------|
| spring-boot-starter-web | REST API |
| spring-boot-starter-validation | Validación de DTOs |
| spring-kafka | Integración con Kafka |
| springdoc-openapi-starter-webmvc-ui | Documentación OpenAPI/Swagger |
| lombok | Reducción de boilerplate |
| spring-boot-starter-test | Testing |
| spring-kafka-test | Testing con Kafka embebido |

## 🚀 Inicio Rápido

### Prerequisitos

- **Java 21** o superior
- **Maven 3.9+**
- **Docker** y **Docker Compose** (para Kafka local)

### 1. Iniciar Kafka (KRaft Mode)

El servicio incluye un `docker-compose.yml` que levanta Kafka en modo KRaft (sin Zookeeper):

```bash
# Desde el directorio backend/broker
docker-compose up -d

# Verificar que Kafka está corriendo
docker-compose ps

# Ver logs de Kafka
docker-compose logs -f kafka
```

> **Nota:** Kafka UI estará disponible en http://localhost:8080 para monitoreo.

### 2. Compilar el Proyecto

```bash
# Compilar sin tests
mvn clean compile

# Compilar con tests (requiere Kafka corriendo o usa Kafka embebido)
mvn clean install

# Solo ejecutar tests
mvn test
```

### 3. Ejecutar el Servicio

```bash
# Opción 1: Con Maven
mvn spring-boot:run

# Opción 2: Como JAR ejecutable
mvn clean package -DskipTests
java -jar target/broker-service-0.0.1-SNAPSHOT.jar
```

El servicio estará disponible en: **http://localhost:8084**

## 📖 API Endpoints

### Health Check

```bash
GET /health
```

**Respuesta:**
```json
{
  "status": "ok"
}
```

**Ejemplo con curl:**
```bash
curl -X GET http://localhost:8084/health
```

### Publicar Mensaje

```bash
POST /publish
Content-Type: application/json
```

**Request Body:**
```json
{
  "topic": "demo-topic",
  "message": "Hello from DocFlow!"
}
```

**Respuesta Exitosa (200 OK):**
```json
{
  "status": "sent",
  "topic": "demo-topic",
  "partition": 0,
  "offset": 42,
  "timestamp": "2025-12-31T10:30:00.123Z"
}
```

**Ejemplo con curl:**
```bash
curl -X POST http://localhost:8084/publish \
  -H "Content-Type: application/json" \
  -d '{"topic": "demo-topic", "message": "Hello from DocFlow!"}'
```

**Respuesta de Error de Validación (400 Bad Request):**
```json
{
  "status": "error",
  "message": "Validation failed",
  "errors": {
    "topic": "Topic is required",
    "message": "Message is required"
  },
  "timestamp": "2025-12-31T10:30:00.123Z"
}
```

## 📚 Documentación OpenAPI

La documentación interactiva de la API está disponible en:

- **Swagger UI:** http://localhost:8084/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8084/api-docs
- **OpenAPI YAML:** http://localhost:8084/api-docs.yaml

## ⚙️ Configuración

### Variables de Entorno

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `KAFKA_BOOTSTRAP_SERVERS` | Servidores Kafka | `localhost:9092` |
| `BROKER_DEMO_TOPIC` | Topic para el listener demo | `demo-topic` |
| `BROKER_CONSUMER_GROUP_ID` | Group ID del consumidor | `broker-demo-group` |

### application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

server:
  port: 8084

broker:
  demo-topic: ${BROKER_DEMO_TOPIC:demo-topic}
```

## 🐳 Docker Compose - Kafka KRaft

El archivo `docker-compose.yml` incluye:

### Kafka (KRaft Mode)
- **Puerto:** 9092
- **Imagen:** apache/kafka:3.7.0
- **Modo:** KRaft (sin Zookeeper)
- **Auto-create topics:** Habilitado

### Kafka UI (Opcional)
- **Puerto:** 8080
- **URL:** http://localhost:8080
- **Imagen:** provectuslabs/kafka-ui:latest

### Comandos Útiles

```bash
# Iniciar todos los servicios
docker-compose up -d

# Detener todos los servicios
docker-compose down

# Detener y eliminar volúmenes
docker-compose down -v

# Ver logs en tiempo real
docker-compose logs -f

# Reiniciar Kafka
docker-compose restart kafka

# Verificar estado de salud
docker-compose ps
```

## 📁 Estructura del Proyecto

```
backend/broker/
├── pom.xml                                    # Configuración Maven
├── README.md                                  # Este archivo
├── docker-compose.yml                         # Kafka en KRaft mode
└── src/
    ├── main/
    │   ├── java/com/docflow/broker/
    │   │   ├── BrokerApplication.java         # Clase principal
    │   │   ├── application/
    │   │   │   └── dto/
    │   │   │       ├── PublishRequest.java    # DTO de request
    │   │   │       └── PublishResponse.java   # DTO de response
    │   │   └── infrastructure/
    │   │       ├── adapters/
    │   │       │   └── input/
    │   │       │       ├── kafka/
    │   │       │       │   └── DemoListener.java
    │   │       │       └── rest/
    │   │       │           ├── HealthController.java
    │   │       │           └── PublishController.java
    │   │       └── config/
    │   │           ├── GlobalExceptionHandler.java
    │   │           └── KafkaConfig.java
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── java/com/docflow/broker/
        │   └── BrokerApplicationTests.java
        └── resources/
            └── application-test.yml
```

## 🧪 Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests con reporte detallado
mvn test -Dtest=BrokerApplicationTests

# Skip tests durante build
mvn clean install -DskipTests
```

### Test Manual con curl

1. **Verificar servicio:**
   ```bash
   curl http://localhost:8084/health
   ```

2. **Publicar mensaje:**
   ```bash
   curl -X POST http://localhost:8084/publish \
     -H "Content-Type: application/json" \
     -d '{"topic": "demo-topic", "message": "Test message"}'
   ```

3. **Ver logs del DemoListener:**
   ```bash
   # En la consola del servicio verás:
   # ======================================
   # Received message from Kafka:
   #   Topic: demo-topic
   #   Partition: 0
   #   Offset: 0
   #   Key: null
   #   Value: Test message
   #   Timestamp: 1735646400000
   # ======================================
   ```

## 🔧 Próximos Pasos

- [ ] Implementar autenticación/autorización
- [ ] Agregar soporte para mensajes con keys
- [ ] Implementar patrones de retry con backoff
- [ ] Agregar métricas con Micrometer
- [ ] Configurar Dead Letter Queue (DLQ)
- [ ] Implementar compresión de mensajes
- [ ] Agregar soporte para transacciones Kafka

---

**Puerto del Servicio:** `8084`  
**Versión:** `0.0.1-SNAPSHOT`
