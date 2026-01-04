
# Identity Service

Servicio de Identidad (IAM) para DocFlow — Implementación de login multi-organización (US-AUTH-001).

---

## 🚀 Resumen Ejecutivo (US-AUTH-001)

**Estado:** ✅ Implementado y compilado con éxito (Java 21, Spring Boot 3.5.0)

**Funcionalidad principal:**
- Login de usuario soportando múltiples organizaciones
- Selección automática de organización por defecto
- Cambio de organización vía endpoint
- Seguridad JWT (stateless, HMAC-SHA256)
- Manejo de errores y validaciones robustas

---

## 🏗️ Arquitectura Hexagonal

```
src/main/java/com/docflow/identity/
├── domain/           # Lógica de negocio pura
│   ├── model/        # Entidades y enums
│   └── exceptions/   # Excepciones de dominio
├── application/      # Casos de uso, DTOs, puertos
│   ├── dto/
│   ├── ports/output/ # Repositorios
│   └── services/     # Servicios de aplicación
└── infrastructure/   # Adaptadores externos
    ├── config/       # Configuración Spring/JWT
    └── adapters/input/rest/ # Controladores REST
```

---

## 🔑 Endpoints REST

- **POST** `/api/v1/auth/login` — Login multi-organización
- **POST** `/api/v1/auth/switch` — Cambio de organización activa

Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

---

## 📦 Entregables y Cobertura

- **Entidades:** Usuario, Organización, Membresía, enums de estado
- **DTOs:** LoginRequest, LoginResponse, SwitchOrganizationRequest
- **Servicios:** Validación de credenciales, resolución de organización, generación de JWT, orquestación de login y cambio de organización
- **Excepciones:** Manejo de credenciales inválidas, sin organizaciones, configuración inválida, organización no encontrada
- **Configuración:** Spring Security, JWT, datasource PostgreSQL
- **Pruebas:**
  - Unitarias (servicio de resolución de organización)
  - Integración (login y cambio de organización, usando Testcontainers y PostgreSQL real)

---

## 🧪 Pruebas y Criterios de Aceptación

| Escenario | Estado | Resumen |
|-----------|--------|---------|
| Usuario con 1 organización | ✅ | Devuelve esa organización |
| Usuario con varias y default | ✅ | Devuelve la default |
| Usuario con 2+ sin default | ✅ | 409 CONFLICT |
| Cambio de organización | ✅ | Nuevo token con org_id |
| Credenciales inválidas | ✅ | 401 UNAUTHORIZED |
| Sin organizaciones activas | ✅ | 403 FORBIDDEN |

**Pruebas manuales:**
- Login exitoso, login con múltiples organizaciones, credenciales inválidas, usuario sin organizaciones, cambio de organización

**Pruebas automáticas:**
- `mvn test` (requiere Docker Desktop para integración)

---

## 🔐 Seguridad

- Hash de contraseñas: BCrypt
- JWT firmado (HMAC-SHA256)
- Sesiones stateless
- Soft delete de usuarios
- Validación de entrada y manejo de errores OWASP

---

## 📝 Despliegue y Uso

1. **Requisitos:** Java 21, Maven 3.8+, Docker Desktop, PostgreSQL 16
2. **Levantar base de datos:**
   ```powershell
   docker compose up -d postgres
   psql -h localhost -U docflow -d docflow -f db/DB_AUTH_1.sql
   ```
3. **Compilar:**
   ```powershell
   cd backend/identity
   mvn clean compile
   ```
4. **Ejecutar:**
   ```powershell
   mvn spring-boot:run
   ```
5. **Probar endpoints:**
   - [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

---

## 🐛 Notas y Problemas Conocidos

- Unit tests fallan en Java 25 (usar Java 21)
- Pruebas de integración requieren Docker Desktop

---

## 🚦 Próximos Pasos (Roadmap)

- US-AUTH-002: Agregar roles[] al JWT
- US-AUTH-003: Middleware JWT para endpoints protegidos
- US-AUTH-004: Aislamiento multi-tenant
- US-AUTH-005: UI de login en React
- US-AUTH-006: Soporte MFA

---

**Compilación:**

```
[INFO] BUILD SUCCESS
```

---

**Desarrollador:** AI Assistant (Claude Sonnet 4.5)

**Fecha:** 4 de enero de 2026

---

Proyecto interno - DocFlow DMS
