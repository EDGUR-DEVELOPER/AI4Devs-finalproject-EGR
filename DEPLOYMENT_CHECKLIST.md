# 🚀 Checklist de Despliegue - DocFlow

Este documento proporciona una guía paso a paso para verificar que el entorno Docker de DocFlow esté funcionando correctamente.

## 📋 Tabla de Contenidos

- [Pre-requisitos](#pre-requisitos)
- [1. Preparación del Entorno](#1-preparación-del-entorno)
- [2. Construcción y Despliegue](#2-construcción-y-despliegue)
- [3. Verificación de Servicios](#3-verificación-de-servicios)
- [4. Pruebas de Endpoints](#4-pruebas-de-endpoints)
- [5. Validación de Logs](#5-validación-de-logs)
- [6. Limpieza y Troubleshooting](#6-limpieza-y-troubleshooting)

---

## Pre-requisitos

Antes de comenzar, verificar que tienes instalado:

```bash
# Verificar Docker
docker --version
# Esperado: Docker version 20.10.x o superior

# Verificar Docker Compose
docker compose version
# Esperado: Docker Compose version v2.x.x o superior
```

**Puertos que deben estar disponibles:**
- `80` - Frontend
- `5432` - PostgreSQL
- `8080` - Gateway (único punto de entrada al backend)
- `9000` - MinIO API
- `9001` - MinIO Console

**Nota de Seguridad:** Los servicios Identity (8081) y Document Core (8082) NO están expuestos externamente. Solo son accesibles internamente a través del Gateway.

---

## 1. Preparación del Entorno

### ✅ 1.1. Crear archivo de configuración

```bash
# Copiar plantilla de variables de entorno
cp .env.example .env

# Editar valores si es necesario (opcional para desarrollo local)
# Los valores por defecto funcionan para desarrollo
```

### ✅ 1.2. Verificar espacio en disco

```bash
# Verificar espacio disponible (mínimo 5GB recomendado)
docker system df
```

---

## 2. Construcción y Despliegue

### ✅ 2.1. Construir todas las imágenes

```bash
# Primera vez: construir todas las imágenes
docker compose build --no-cache

# Verificar que las imágenes se crearon correctamente
docker images | grep docflow
```

**Imágenes esperadas:**
- `docflow-gateway`
- `docflow-identity`
- `docflow-document-core`
- `docflow-frontend`

### ✅ 2.2. Iniciar todos los servicios

```bash
# Iniciar en modo detached (background)
docker compose up -d

# Esperar ~30-60 segundos para que todos los servicios arranquen
```

### ✅ 2.3. Verificar estado de contenedores

```bash
# Ver estado de todos los servicios
docker compose ps

# Todos los servicios deben mostrar estado "Up" o "healthy"
```

**Salida esperada:**
```
NAME                    STATUS
docflow-frontend        Up (healthy)
docflow-gateway         Up (healthy)
docflow-identity        Up (healthy)
docflow-document-core   Up (healthy)
docflow-postgres        Up (healthy)
docflow-minio           Up (healthy)
```

---

## 3. Verificación de Servicios

### ✅ 3.1. Verificar red Docker

```bash
# Verificar que la red existe
docker network ls | grep docflow

# Verificar conectividad entre servicios
docker compose exec gateway-service ping -c 2 postgres
docker compose exec gateway-service ping -c 2 identity-service
```

### ✅ 3.2. Verificar volúmenes

```bash
# Verificar que los volúmenes fueron creados
docker volume ls | grep docflow

# Esperado:
# - docflow-postgres-data
# - docflow-minio-data
```

---

## 4. Pruebas de Endpoints

### ✅ 4.1. Infraestructura

#### PostgreSQL
```bash
# Conectar a PostgreSQL
docker compose exec postgres psql -U docflow -d docflow -c "\l"

# Debe mostrar lista de bases de datos incluyendo 'docflow'
```

#### MinIO
```bash
# Acceder a consola web de MinIO
# URL: http://localhost:9001
# Usuario: minioadmin
# Password: minioadmin123

# Verificar que existe el bucket 'docflow-documents'
```

### ✅ 4.2. Backend Services

#### Gateway Service
```bash
# Health check
curl -i http://localhost:8080/actuator/health

# Esperado: HTTP/1.1 200 OK
# {"status":"UP"}
```

```bash
# Swagger UI
# URL: http://localhost:8080/webjars/swagger-ui/index.html
# Debe cargar la interfaz de Swagger
```

#### Identity Service
```bash
# NOTA: Este servicio NO está expuesto externamente
# Solo accesible a través del Gateway mediante rutas configuradas
# Ejemplo de acceso vía Gateway:
curl -i http://localhost:8080/api/identity/actuator/health

# O verificar internamente desde el contenedor:
docker compose exec identity-service wget -qO- http://localhost:8081/actuator/health
```

#### Document Core Service
```bash
# NOTA: Este servicio NO está expuesto externamente
# Solo accesible a través del Gateway mediante rutas configuradas
# Ejemplo de acceso vía Gateway:
curl -i http://localhost:8080/api/documents/actuator/health

# O verificar internamente desde el contenedor:
docker compose exec document-core-service wget -qO- http://localhost:8082/actuator/health
```

### ✅ 4.3. Frontend

#### Acceso a aplicación web
```bash
# Health check
curl -i http://localhost/health

# Esperado: HTTP/1.1 200 OK
# healthy
```

```bash
# Página principal
# URL: http://localhost
# Debe cargar la aplicación React
```

#### PowerShell (Windows)
```powershell
# Health check
Invoke-WebRequest -Uri http://localhost/health -Method GET

# Página principal
Start-Process "http://localhost"
```

---

## 5. Validación de Logs

### ✅ 5.1. Ver logs en tiempo real

```bash
# Logs de todos los servicios
docker compose logs -f

# Logs de un servicio específico
docker compose logs -f gateway-service
docker compose logs -f identity-service
docker compose logs -f document-core-service
docker compose logs -f frontend
```

### ✅ 5.2. Verificar arranque exitoso

**Buscar en logs de cada servicio:**

#### Gateway Service
```
Started GatewayServiceApplication
```

#### Identity Service
```
Started IdentityServiceApplication
HikariPool-1 - Start completed
```

#### Document Core Service
```
Started DocumentCoreServiceApplication
```

#### Frontend (Nginx)
```
start worker processes
```

### ✅ 5.3. Verificar errores comunes

```bash
# Buscar errores en logs
docker compose logs | grep -i error
docker compose logs | grep -i exception

# No debe haber errores críticos en el arranque
```

---

## 6. Limpieza y Troubleshooting

### 🔧 6.1. Reiniciar un servicio específico

```bash
# Reiniciar un servicio
docker compose restart gateway-service

# Reconstruir y reiniciar
docker compose up -d --build gateway-service
```

### 🔧 6.2. Ver logs de un servicio fallido

```bash
# Ver últimas 100 líneas
docker compose logs --tail=100 identity-service

# Ver logs con timestamps
docker compose logs -t gateway-service
```

### 🔧 6.3. Detener todos los servicios

```bash
# Detener servicios (mantiene volúmenes)
docker compose down

# Detener y eliminar volúmenes (CUIDADO: elimina datos)
docker compose down -v
```

### 🔧 6.4. Limpieza completa

```bash
# Eliminar contenedores, redes, volúmenes e imágenes
docker compose down -v --rmi all --remove-orphans

# Limpiar sistema Docker completo (CUIDADO: afecta otros proyectos)
docker system prune -a --volumes
```

### 🔧 6.5. Reconstruir desde cero

```bash
# Limpieza completa
docker compose down -v --rmi all

# Reconstruir y levantar
docker compose build --no-cache
docker compose up -d

# Verificar estado
docker compose ps
```

---

## 📊 Resumen de Comandos Rápidos

```bash
# Levantar entorno
cp .env.example .env
docker compose up --build -d

# Verificar estado
docker compose ps
docker compose logs -f

# Pruebas rápidas
curl http://localhost/health                    # Frontend
curl http://localhost:8080/actuator/health     # Gateway
curl http://localhost:8081/actuator/health     # Identity
curl http://localhost:8082/actuator/health     # Document Core

# Detener
docker compose down

# Limpiar todo
docker compose down -v --rmi all --remove-orphans
```

---

## ✅ Checklist de Aceptación

Marcar cada item cuando esté verificado:

- [ ] Todas las imágenes Docker se construyeron sin errores
- [ ] Todos los contenedores están en estado "Up" y "healthy"
- [ ] PostgreSQL es accesible y tiene la base de datos 'docflow'
- [ ] MinIO Console es accesible en http://localhost:9001
- [ ] MinIO tiene el bucket 'docflow-documents' creado
- [ ] Gateway Service responde en `/actuator/health` (200 OK)
- [ ] Identity Service es accesible solo internamente (verificar con docker exec)
- [ ] Document Core Service es accesible solo internamente (verificar con docker exec)
- [ ] Gateway enruta correctamente a servicios internos
- [ ] Frontend es accesible en http://localhost (200 OK)
- [ ] Frontend responde en `/health` (200 OK)
- [ ] No hay errores críticos en los logs de ningún servicio
- [ ] Swagger UI es accesible para todos los servicios backend

---

## 🆘 Soporte

Si encuentras problemas:

1. Revisa los logs: `docker compose logs -f <servicio>`
2. Verifica el estado: `docker compose ps`
3. Consulta el [README-docker.md](README-docker.md) para más detalles
4. Revisa la configuración en `.env`
5. Verifica que todos los puertos estén disponibles

---

## 📚 Referencias

- [README.md](README.md) - Documentación general del proyecto
- [README-docker.md](README-docker.md) - Documentación de infraestructura Docker
- [docker-compose.yml](docker-compose.yml) - Configuración de orquestación
- [.env.example](.env.example) - Plantilla de variables de entorno
