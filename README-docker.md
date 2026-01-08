# 🐳 DocFlow - Infraestructura Docker Compose

Este documento describe la configuración de Docker Compose para el entorno de desarrollo local de **DocFlow**. Proporciona todos los servicios de infraestructura necesarios para ejecutar los microservicios backend.

## 📋 Índice

- [Propósito](#propósito)
- [Prerequisitos](#prerequisitos)
- [Configuración](#configuración)
- [Comandos de Uso](#comandos-de-uso)
- [Servicios Incluidos](#servicios-incluidos)
- [Interfaces Web](#interfaces-web)
- [Conexión desde Microservicios](#conexión-desde-microservicios)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Propósito

Este `docker-compose.yml` levanta la infraestructura necesaria para desarrollo local, basada en el [Diagrama de Arquitectura Local](README.md#diagrama-de-arquitectura-local-docker-compose):

```
┌─────────────────────────────────────────────────────────────────┐
│                    Máquina Local (Docker)                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │PostgreSQL│  │ MongoDB  │  │  MinIO   │  │  Redis   │        │
│  │  :5432   │  │  :27017  │  │:9000/9001│  │  :6379   │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │  Kafka   │  │ Kafka UI │  │  Vault   │                      │
│  │  :9092   │  │  :9090   │  │  :8200   │                      │
│  └──────────┘  └──────────┘  └──────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

| Servicio | Microservicio que lo usa |
|----------|--------------------------|
| PostgreSQL | `identity-service`, `document-core-service` |
| MongoDB | `auditlog-service` |
| MinIO | `document-core-service` |
| Redis | `document-core-service` |
| Kafka | `broker-service`, todos los microservicios (eventos) |
| Vault | Todos los microservicios (secretos) |

---

## ✅ Prerequisitos

- **Docker Desktop** ≥ 4.0 (Windows/Mac) o **Docker Engine** ≥ 20.10 (Linux)
- **Docker Compose** ≥ 2.0 (incluido en Docker Desktop)
- **Puertos disponibles**: 5432, 6379, 8200, 9000, 9001, 9090, 9092, 9093, 27017

### Verificar instalación

```bash
docker --version
docker compose version
```

---

## ⚙️ Configuración

### 1. Crear archivo de entorno

```bash
# Copiar plantilla de ejemplo
cp .env.example .env

# Editar según necesidades (opcional para desarrollo)
# Los valores por defecto funcionan para desarrollo local
```

### 2. Variables de entorno principales

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `POSTGRES_USER` | `docflow` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | `docflow_secret` | Contraseña de PostgreSQL |
| `POSTGRES_DB` | `docflow` | Base de datos inicial |
| `MONGO_DATABASE` | `auditlog` | Base de datos de MongoDB |
| `MINIO_ROOT_USER` | `minioadmin` | Usuario admin de MinIO |
| `MINIO_ROOT_PASSWORD` | `minioadmin123` | Contraseña de MinIO |
| `VAULT_DEV_ROOT_TOKEN` | `root` | Token de Vault (modo dev) |

### 3. Variables de entorno para Gateway (CORS)

El servicio Gateway requiere configuración CORS para permitir requests desde el frontend.

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Lista separada por comas de orígenes permitidos para CORS |

#### Configuración por Entorno

**Desarrollo (local):**
```yaml
# docker-compose.yml o .env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

**Staging:**
```yaml
CORS_ALLOWED_ORIGINS=https://staging.docflow.com,https://admin-staging.docflow.com
```

**Producción:**
```yaml
CORS_ALLOWED_ORIGINS=https://app.docflow.com,https://admin.docflow.com
```

#### ⚠️ Restricciones de Seguridad

- **NO usar wildcard (`*`)**: El wildcard está prohibido por razones de seguridad y causará que el Gateway falle al iniciar.
- **Protocolo completo requerido**: Cada origen debe incluir `http://` o `https://`.
- **Sin espacios**: Los orígenes deben estar separados por comas sin espacios.
- **Warning en producción**: El Gateway registrará warnings si detecta orígenes `localhost` cuando el perfil activo es `production`.

#### Ejemplos Válidos

✅ **Correcto:**
```bash
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
CORS_ALLOWED_ORIGINS=https://app.docflow.com,https://admin.docflow.com
```

❌ **Incorrecto:**
```bash
CORS_ALLOWED_ORIGINS=*                              # Wildcard prohibido
CORS_ALLOWED_ORIGINS=localhost:5173                 # Falta protocolo
CORS_ALLOWED_ORIGINS=http://localhost:5173, http://localhost:3000  # Espacios no permitidos
```

#### Aplicar cambios CORS

Si modificas la variable `CORS_ALLOWED_ORIGINS`, debes reiniciar el servicio Gateway:

```bash
# Opción 1: Reiniciar solo Gateway (si está corriendo en Docker)
docker compose restart gateway

# Opción 2: Reiniciar aplicación Spring Boot (si corre localmente)
# Ctrl+C y volver a ejecutar mvn spring-boot:run en backend/gateway
```

---

## 🚀 Comandos de Uso

### Iniciar servicios

```bash
# Iniciar todos los servicios en segundo plano
docker compose up -d

# Ver logs en tiempo real
docker compose logs -f

# Ver logs de un servicio específico
docker compose logs -f postgres
```

### Verificar estado

```bash
# Ver estado de contenedores
docker compose ps

# Ver salud de servicios
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
```

### Detener servicios

```bash
# Detener servicios (mantiene datos en volúmenes)
docker compose down

# Detener y eliminar volúmenes (BORRA TODOS LOS DATOS)
docker compose down -v

# Detener un servicio específico
docker compose stop postgres
```

### Reiniciar servicios

```bash
# Reiniciar todos los servicios
docker compose restart

# Reiniciar un servicio específico
docker compose restart kafka
```

### Limpiar recursos

```bash
# Eliminar contenedores, redes y volúmenes
docker compose down -v --remove-orphans

# Limpiar imágenes no utilizadas
docker image prune -f
```

---

## 📦 Servicios Incluidos

### PostgreSQL (Base de Datos Relacional)

| Propiedad | Valor |
|-----------|-------|
| **Puerto** | `5432` |
| **Imagen** | `postgres:16-alpine` |
| **Usuario** | `docflow` |
| **Contraseña** | `docflow_secret` |
| **Base de datos** | `docflow` |
| **Volumen** | `docflow-postgres-data` |

**Crear bases de datos adicionales** (después de iniciar):
```bash
# Conectar al contenedor
docker exec -it docflow-postgres psql -U docflow -d docflow

# Crear bases de datos para microservicios
CREATE DATABASE docflow_identity;
CREATE DATABASE docflow_documents;
\q
```

---

### MongoDB (Base de Datos NoSQL)

| Propiedad | Valor |
|-----------|-------|
| **Puerto** | `27017` |
| **Imagen** | `mongo:7.0` |
| **Autenticación** | Deshabilitada (dev) |
| **Base de datos** | `auditlog` |
| **Volumen** | `docflow-mongo-data` |

**Conectar con mongosh**:
```bash
docker exec -it docflow-mongodb mongosh
```

---

### MinIO (Object Storage S3-Compatible)

| Propiedad | Valor |
|-----------|-------|
| **Puerto API** | `9000` |
| **Puerto Console** | `9001` |
| **Imagen** | `minio/minio:RELEASE.2024-01-01T16-36-33Z` |
| **Usuario** | `minioadmin` |
| **Contraseña** | `minioadmin123` |
| **Bucket** | `docflow-documents` (creado automáticamente) |
| **Volumen** | `docflow-minio-data` |

**Endpoint S3**: `http://localhost:9000`

---

### Redis (Cache en Memoria)

| Propiedad | Valor |
|-----------|-------|
| **Puerto** | `6379` |
| **Imagen** | `redis:7-alpine` |
| **Autenticación** | Deshabilitada (dev) |
| **Persistencia** | AOF habilitado |
| **Volumen** | `docflow-redis-data` |

**Conectar con redis-cli**:
```bash
docker exec -it docflow-redis redis-cli
```

---

### Apache Kafka (Message Broker)

| Propiedad | Valor |
|-----------|-------|
| **Puerto Broker** | `9092` |
| **Puerto Controller** | `9093` |
| **Imagen** | `apache/kafka:3.7.0` |
| **Modo** | KRaft (sin Zookeeper) |
| **Cluster ID** | `MkU3OEVBNTcwNTJENDM2Qk` |
| **Volumen** | `docflow-kafka-data` |

**Bootstrap servers**: `localhost:9092`

---

### HashiCorp Vault (Secrets Management)

| Propiedad | Valor |
|-----------|-------|
| **Puerto** | `8200` |
| **Imagen** | `hashicorp/vault:1.17` |
| **Modo** | Desarrollo (dev) |
| **Token** | `root` |
| **Volumen** | `docflow-vault-data` |

⚠️ **Advertencia**: El modo desarrollo NO es seguro para producción.

---

## 🌐 Interfaces Web

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| **MinIO Console** | http://localhost:9001 | `minioadmin` / `minioadmin123` |
| **Kafka UI** | http://localhost:9090 | Sin autenticación |
| **Vault UI** | http://localhost:8200 | Token: `root` |

---

## 🔌 Conexión desde Microservicios

### application.yml para Identity/Document-Core Service

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/docflow_identity
    username: docflow
    password: docflow_secret
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### application.yml para Audit Log Service

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/auditlog
      database: auditlog
```

### application.yml para Document Core (MinIO/S3)

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin123
  bucket: docflow-documents
```

### application.yml para Redis Cache

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### application.yml para Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ${spring.application.name}-group
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### application.yml para Vault

```yaml
spring:
  cloud:
    vault:
      uri: http://localhost:8200
      token: root
      kv:
        enabled: true
        backend: secret
        default-context: docflow
```

---

## 🔧 Troubleshooting

### Puerto en uso

```bash
# Windows - Encontrar proceso usando el puerto
netstat -ano | findstr :5432
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :5432
kill -9 <PID>
```

### Contenedor no inicia

```bash
# Ver logs del contenedor
docker compose logs postgres

# Reiniciar contenedor
docker compose restart postgres

# Recrear contenedor
docker compose up -d --force-recreate postgres
```

### Problemas de permisos en volúmenes (Linux)

```bash
# Verificar permisos
ls -la /var/lib/docker/volumes/

# Ajustar permisos (si es necesario)
sudo chown -R 1000:1000 /var/lib/docker/volumes/docflow-*
```

### Kafka no arranca

```bash
# Verificar logs
docker compose logs kafka

# Eliminar datos corruptos y reiniciar
docker compose down -v
docker volume rm docflow-kafka-data
docker compose up -d
```

### Limpiar todo y empezar de cero

```bash
# Eliminar contenedores, volúmenes, redes e imágenes
docker compose down -v --rmi all --remove-orphans

# Reconstruir desde cero
docker compose up -d --build
```

---

## 📏 Reglas de infraestructura y Docker

Las reglas específicas para mantener y evolucionar la infraestructura definida en los archivos `docker-compose.yml` se describen en:

- [.github/rules-infra-docker.md](.github/rules-infra-docker.md)

## 📊 Resumen de Puertos

| Puerto | Servicio | Protocolo |
|--------|----------|-----------|
| 5432 | PostgreSQL | TCP |
| 6379 | Redis | TCP |
| 8200 | Vault | HTTP |
| 9000 | MinIO API | HTTP |
| 9001 | MinIO Console | HTTP |
| 9090 | Kafka UI | HTTP |
| 9092 | Kafka Broker | TCP |
| 9093 | Kafka Controller | TCP |
| 27017 | MongoDB | TCP |

---

## 📝 Notas

- Los datos persisten en volúmenes Docker nombrados (`docflow-*-data`)
- Para desarrollo, las credenciales por defecto son suficientes
- **No usar estas configuraciones en producción**
- El bucket de MinIO `docflow-documents` se crea automáticamente al iniciar
