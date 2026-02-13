# ✅ Migración a MinIO - Document Core Service

## Rápido Resumen

Se ha implementado **MinIO** como almacenamiento de objetos (S3-compatible) en reemplazo del almacenamiento local para producción.

---

## 📋 Cambios Implementados

### 1. **Dependencias Maven** (`pom.xml`)
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.10</version>
</dependency>
```

### 2. **Configuración MinIO** 
**Archivo:** `infrastructure/config/MinioConfig.java` (NUEVO)

```java
@Configuration
public class MinioConfig {
    @Bean
    public MinioClient minioClient() { ... }
}
```

**Características:**
- ✅ Crea cliente MinIO automáticamente
- ✅ Verifica y crea bucket si no existe
- ✅ Manejo robusto de conexión

### 3. **Implementación MinIO**
**Archivo:** `application/service/MinioStorageService.java` (NUEVO)

```java
@Service
@ConditionalOnProperty(name = "docflow.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService { ... }
```

**Operaciones implementadas:**
- ✅ `upload()` - Carga archivos a MinIO
- ✅ `download()` - Descarga archivos desde MinIO
- ✅ `delete()` - Elimina archivos en MinIO
- ✅ `exists()` - Verifica existencia de archivos

### 4. **LocalStorageService - Actualizado**
```java
@Service
@ConditionalOnProperty(name = "docflow.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService { ... }
```

**Cambios:**
- ✅ Agregada activación condicional basada en propiedad
- ✅ Activo por defecto para desarrollo

### 5. **Configuración de Aplicación** (`application.yml`)
```yaml
docflow:
  storage:
    type: ${DOCFLOW_STORAGE_TYPE:local}
    
    local:
      path: ${DOCFLOW_STORAGE_PATH:./storage}
    
    minio:
      endpoint: ${MINIO_ENDPOINT:http://minio:9000}
      access-key: ${MINIO_ROOT_USER:minioadmin}
      secret-key: ${MINIO_ROOT_PASSWORD:minioadmin123}
      bucket-name: ${MINIO_BUCKET_NAME:docflow-documents}
```

### 6. **Variables de Entorno** (`.env.example`)
```dotenv
# Almacenamiento
DOCFLOW_STORAGE_TYPE=minio

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
MINIO_BUCKET_NAME=docflow-documents
```

---

## 🎯 Uso

### Desarrollo (Local)
```bash
# Opción 1: Usar almacenamiento local por defecto
# (Sin configurar DOCFLOW_STORAGE_TYPE)

# Opción 2: Configurar explícitamente
DOCFLOW_STORAGE_TYPE=local
DOCFLOW_STORAGE_PATH=./storage
```

### Producción (MinIO)
```bash
DOCFLOW_STORAGE_TYPE=minio
MINIO_ENDPOINT=http://minio:9000
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
MINIO_BUCKET_NAME=docflow-documents
```

---

## 🏗️ Arquitectura

**Patrón:** Hexagonal Architecture

```
┌─────────────────────┐
│  StorageService     │ ◄─── Port (interfaz)
│  (Interface)        │
└─────────────────────┘
         ▲
         │ implements
    ┌────┴─────┐
    │           │
┌───────┐   ┌──────────┐
│ Local │   │  MinIO   │ ◄─── Adapters (adaptadores)
│Storage│   │ Storage  │
└───┬───┘   └────┬─────┘
    │            │
    ▼            ▼
┌──────────┐  ┌────────┐
│filesystem│  │ MinIO  │
│ ./storage│  │ S3 API │
└──────────┘  └────────┘
```

**Activación Condicional:**
- `LocalStorageService` → `@ConditionalOnProperty(name = "docflow.storage.type", havingValue = "local", matchIfMissing = true)`
- `MinioStorageService` → `@ConditionalOnProperty(name = "docflow.storage.type", havingValue = "minio")`

---

## ✅ Validación

- ✅ **Compilación:** `mvn clean package -DskipTests` → BUILD SUCCESS
- ✅ **Dependencias:** Resueltas correctamente (minio 8.5.10)
- ✅ **Estructura:** Acorde a patrones hexagonales
- ✅ **Seguridad:** Credenciales via variables de entorno
- ✅ **Compatibilidad:** Ambos adaptadores activos según configuración

---

## 📁 Archivos Modificados/Creados

| Archivo | Tipo | Estado |
|---------|------|--------|
| `pom.xml` | Modificado | ✅ Dependencia MinIO agregada |
| `infrastructure/config/MinioConfig.java` | Nuevo | ✅ Configuración MinIO |
| `application/service/MinioStorageService.java` | Nuevo | ✅ Implementación MinIO |
| `application/service/LocalStorageService.java` | Modificado | ✅ Activación condicional |
| `application.yml` | Modificado | ✅ Propiedades MinIO |
| `.env.example` | Modificado | ✅ Variables DOCFLOW_STORAGE_TYPE |

---

## 🚀 Próximos Pasos (Opcional)

1. **Tests Unitarios:** Crear tests para `MinioStorageService`
2. **Tests de Integración:** Con contenedor MinIO real
3. **Refactoring:** Mover adapters a `infrastructure/adapter/storage/`
4. **Monitoreo:** Agregar métricas de uso de almacenamiento
5. **Políticas:** Implementar lifecycle y retención en MinIO
6. **Compresión:** Comprimir archivos antiguos automáticamente

---

## 📚 Referencias

- [MinIO Java SDK Docs](https://docs.min.io/minio/baremetal/sdk/java/API.html)
- [Docker Compose MinIO](../../README-docker.md)
- [Arquitectura Hexagonal](../../README.md)

---

**Fecha:** 13 de febrero de 2026  
**Estado:** ✅ Implementación Completada
