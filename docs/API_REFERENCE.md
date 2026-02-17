# API Reference - DocFlow

Este documento proporciona la especificación completa de la API REST de DocFlow, incluyendo endpoints, esquemas y ejemplos de uso.

## Especificación OpenAPI

```yaml
openapi: 3.0.3
info:
    title: DocFlow API (Mini OpenAPI - MVP)
    version: 0.1.0
    description: >
        Especificación mínima (MVP) para DocFlow enfocada en:
        autenticación, creación de carpetas y carga de documentos (v1).

servers:
    - url: https://api.docflow.local
        description: Entorno local/dev (placeholder)
    - url: http://localhost:8080
        description: Desarrollo local

tags:
    - name: autenticacion
        description: Inicio de sesión, cambio de organización y emisión de token
    - name: carpetas
        description: Gestión mínima de carpetas
    - name: documentos
        description: Carga y gestión de documentos

paths:
    /auth/login:
        post:
            tags: [autenticacion]
            summary: Iniciar sesión y obtener token (organización predeterminada)
            description: >
                Autentica credenciales.
                La organización activa se resuelve por la membresía marcada como `es_predeterminada=true`.
                Reglas MVP:
                - Si el usuario tiene 1 organización activa, el sistema emite el token para esa organización.
                - Si el usuario tiene 2 organizaciones activas, debe existir exactamente 1 membresía
                  con `es_predeterminada=true` y se emite el token para esa organización.
                - Si NO hay predeterminada (con 2 activas) o el usuario tiene más de 2 organizaciones activas,
                  el sistema devuelve error de configuración (409).
            operationId: login
            requestBody:
                required: true
                content:
                    application/json:
                        schema:
                            $ref: '#/components/schemas/LoginRequest'
                        examples:
                            login:
                                value:
                                    email: admin@acme.com
                                    contrasena: PasswordSegura123!
            responses:
                '200':
                    description: Token emitido correctamente
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/LoginResponse'
                '400':
                    description: Solicitud inválida (campos faltantes/formato inválido)
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '401':
                    description: Credenciales inválidas
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '403':
                    description: Usuario sin membresía activa o usuario desactivado
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '409':
                    description: Configuración de Organizacion inválida (sin predeterminada o exceso de organizaciones)
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'

    /auth/switch:
        post:
            tags: [autenticacion]
            summary: Cambiar organización activa (emite nuevo token)
            description: >
                Emite un nuevo token JWT en el contexto de otra `organizacion_id` a la que el
                usuario autenticado pertenece (membresía activa + organización activa).
                La UI de administración muestra este módulo solo si el usuario tiene más de una
                organización disponible.
            operationId: cambiarOrganizacion
            security:
                - bearerAuth: []
            requestBody:
                required: true
                content:
                    application/json:
                        schema:
                            $ref: '#/components/schemas/SwitchOrgRequest'
                        examples:
                            cambiarAOrg2:
                                value:
                                    organizacion_id: 2
            responses:
                '200':
                    description: Token emitido correctamente
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/LoginResponse'
                '400':
                    description: Solicitud inválida (campos faltantes/formato inválido)
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '401':
                    description: No autenticado
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '403':
                    description: Organización no accesible para el usuario o inactiva
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'

    /carpetas:
        post:
            tags: [carpetas]
            summary: Crear carpeta
            description: Crea una carpeta (raíz o hija). Requiere autenticación y permisos.
            operationId: crearCarpeta
            security:
                - bearerAuth: []
            requestBody:
                required: true
                content:
                    application/json:
                        schema:
                            $ref: '#/components/schemas/CrearCarpetaRequest'
                        examples:
                            carpetaRaiz:
                                value:
                                    nombre: Legal
                            carpetaHija:
                                value:
                                    nombre: Contratos 2025
                                    carpeta_padre_id: 10
            responses:
                '201':
                    description: Carpeta creada
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/CarpetaResponse'
                '400':
                    description: Solicitud inválida (validación de campos)
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '401':
                    description: No autenticado
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '403':
                    description: Sin permisos para crear carpetas
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'

    /documentos:
        post:
            tags: [documentos]
            summary: Subir documento (crea documento y versión v1)
            description: >
                Crea un documento en una carpeta y registra su primera versión.
                Requiere autenticación y permisos de escritura en la carpeta.
            operationId: crearDocumento
            security:
                - bearerAuth: []
            requestBody:
                required: true
                content:
                    multipart/form-data:
                        schema:
                            type: object
                            required: [archivo, nombre, carpeta_id]
                            properties:
                                archivo:
                                    type: string
                                    format: binary
                                    description: Archivo a subir.
                                nombre:
                                    type: string
                                    description: Nombre lógico del documento.
                                    example: Contrato_Acme_2025.pdf
                                carpeta_id:
                                    type: integer
                                    format: int64
                                    description: Identificador de la carpeta destino.
                                    example: 10
                                descripcion:
                                    type: string
                                    description: Descripción opcional del documento.
                                    example: Contrato marco con Acme 2025
                                metadatos:
                                    type: string
                                    description: >
                                        JSON serializado con metadatos globales (tags, cliente, etc.).
                                        Se define como string para mantener simple el multipart.
                                    example: '{"cliente":"Acme Corp","tags":["legal","urgente"]}'
            responses:
                '201':
                    description: Documento creado y versión inicial registrada
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/DocumentoCreadoResponse'
                '400':
                    description: Solicitud inválida (faltan campos o formato no válido)
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '401':
                    description: No autenticado
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '403':
                    description: Sin permisos para escribir en la carpeta
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/Error'
                '404':
                    description: Carpeta no encontrada
                    content:
                        application/json:
                            schema:
                                $ ref: '#/components/schemas/Error'

components:
    securitySchemes:
        bearerAuth:
            type: http
            scheme: bearer
            bearerFormat: JWT

    schemas:
        LoginRequest:
            type: object
            required: [email, contrasena]
            properties:
                email:
                    type: string
                    format: email
                    example: admin@acme.com
                contrasena:
                    type: string
                    format: password
                    example: PasswordSegura123!

        SwitchOrgRequest:
            type: object
            required: [organizacion_id]
            properties:
                organizacion_id:
                    type: integer
                    format: int32
                    description: Identificador de la organización a la que se desea cambiar.
                    example: 2

        OrganizacionDisponible:
            type: object
            required: [organizacion_id, nombre]
            properties:
                organizacion_id:
                    type: integer
                    format: int32
                    example: 1
                nombre:
                    type: string
                    example: Acme Corp

        LoginResponse:
            type: object
            required: [token, tipo_token, expira_en, organizaciones]
            properties:
                token:
                    type: string
                    description: Token para usar en Authorization: Bearer <token>
                tipo_token:
                    type: string
                    example: Bearer
                expira_en:
                    type: integer
                    format: int32
                    description: Segundos hasta expiración del token.
                    example: 3600
                organizaciones:
                    type: array
                    description: Organizaciones a las que pertenece el usuario (membresías activas).
                    items:
                        $ref: '#/components/schemas/OrganizacionDisponible'

        CrearCarpetaRequest:
            type: object
            required: [nombre]
            properties:
                nombre:
                    type: string
                    minLength: 1
                    example: Legal
                carpeta_padre_id:
                    type: integer
                    format: int64
                    nullable: true
                    description: Si es null, la carpeta es raíz.
                    example: 10

        CarpetaResponse:
            type: object
            required: [carpeta_id, nombre, carpeta_padre_id, creado_en]
            properties:
                carpeta_id:
                    type: integer
                    format: int64
                    example: 10
                nombre:
                    type: string
                    example: Legal
                carpeta_padre_id:
                    type: integer
                    format: int64
                    nullable: true
                    example: null
                creado_en:
                    type: string
                    format: date-time
                    example: 2025-12-16T10:15:30Z

        DocumentoCreadoResponse:
            type: object
            required: [documento_id, nombre, carpeta_id, version_actual, creado_en]
            properties:
                documento_id:
                    type: integer
                    format: int64
                    example: 987
                nombre:
                    type: string
                    example: Contrato_Acme_2025.pdf
                carpeta_id:
                    type: integer
                    format: int64
                    example: 10
                version_actual:
                    type: object
                    required: [version_id, numero_secuencial, etiqueta_version]
                    properties:
                        version_id:
                            type: integer
                            format: int64
                            example: 5551
                        numero_secuencial:
                            type: integer
                            format: int32
                            example: 1
                        etiqueta_version:
                            type: string
                            example: v1.0
                creado_en:
                    type: string
                    format: date-time
                    example: 2025-12-16T10:15:30Z

        Error:
            type: object
            required: [codigo, mensaje]
            properties:
                codigo:
                    type: string
                    example: ERROR_VALIDACION
                mensaje:
                    type: string
                    example: El campo 'nombre' es obligatorio.
                detalle:
                    type: object
                    additionalProperties: true
                    description: Datos adicionales opcionales.
```

## Ejemplos de Uso

### Autenticación: POST /auth/login

#### Request

```http
POST /auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
    "email": "admin@acme.com",
    "contrasena": "PasswordSegura123!"
}
```

#### Response 200 - Éxito (usuario con múltiples organizaciones)

```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tipo_token": "Bearer",
    "expira_en": 3600,
    "organizaciones": [
        {"organizacion_id": 1, "nombre": "Acme Corp"},
        {"organizacion_id": 2, "nombre": "Contoso Ltd"}
    ]
}
```

#### Response 401 - Credenciales inválidas

```json
{
    "codigo": "CREDENCIALES_INVALIDAS",
    "mensaje": "Email o contraseña incorrectos."
}
```

#### Response 403 - Usuario sin organizaciones activas

```json
{
    "codigo": "SIN_ORGANIZACION",
    "mensaje": "El usuario no pertenece a ninguna organización activa."
}
```

#### Response 409 - Configuración inválida

```json
{
    "codigo": "ORGANIZACION_CONFIG_INVALIDA",
    "mensaje": "No es posible resolver la organización predeterminada para el login (falta predeterminada o exceso de organizaciones)."
}
```

---

### Cambio de Organización: POST /auth/switch

#### Request

```http
POST /auth/switch HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
    "organizacion_id": 2
}
```

#### Response 200 - Éxito

```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (nuevo token)",
    "tipo_token": "Bearer",
    "expira_en": 3600,
    "organizaciones": [
        {"organizacion_id": 1, "nombre": "Acme Corp"},
        {"organizacion_id": 2, "nombre": "Contoso Ltd"}
    ]
}
```

#### Response 403 - Organización no accesible

```json
{
    "codigo": "ORGANIZACION_NO_ACCESIBLE",
    "mensaje": "No tienes permiso para acceder a la organización especificada."
}
```

---

### Crear Carpeta: POST /carpetas

#### Request - Carpeta Raíz

```http
POST /carpetas HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
    "nombre": "Legal"
}
```

#### Request - Carpeta Hija

```http
POST /carpetas HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
    "nombre": "Contratos 2025",
    "carpeta_padre_id": 10
}
```

#### Response 201 - Carpeta creada

```json
{
    "carpeta_id": 22,
    "nombre": "Contratos 2025",
    "carpeta_padre_id": 10,
    "creado_en": "2025-12-16T10:15:30Z"
}
```

#### Response 400 - Validación fallida

```json
{
    "codigo": "ERROR_VALIDACION",
    "mensaje": "El campo 'nombre' es obligatorio.",
    "detalle": {
        "campo": "nombre",
        "error": "NotNull"
    }
}
```

#### Response 403 - Sin permisos

```json
{
    "codigo": "SIN_PERMISOS",
    "mensaje": "No tienes permisos para crear carpetas en esta ubicación."
}
```

---

### Subir Documento: POST /documentos

#### Request usando cURL

```bash
curl -X POST "http://localhost:8080/documentos" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -F "archivo=@Contrato_Acme_2025.pdf" \
  -F "nombre=Contrato_Acme_2025.pdf" \
  -F "carpeta_id=10" \
  -F "descripcion=Contrato marco con Acme 2025" \
  -F "metadatos={\"cliente\":\"Acme Corp\",\"tags\":[\"legal\",\"urgente\"]}"
```

#### Response 201 - Documento creado

```json
{
    "documento_id": 987,
    "nombre": "Contrato_Acme_2025.pdf",
    "carpeta_id": 10,
    "version_actual": {
        "version_id": 5551,
        "numero_secuencial": 1,
        "etiqueta_version": "v1.0"
    },
    "creado_en": "2025-12-16T10:15:30Z"
}
```

#### Response 400 - Campo faltante

```json
{
    "codigo": "ERROR_VALIDACION",
    "mensaje": "El campo 'carpeta_id' es obligatorio."
}
```

#### Response 403 - Sin permisos de escritura

```json
{
    "codigo": "SIN_PERMISOS_ESCRITURA",
    "mensaje": "No tienes permisos de escritura en la carpeta especificada."
}
```

#### Response 404 - Carpeta no encontrada

```json
{
    "codigo": "CARPETA_NO_ENCONTRADA",
    "mensaje": "La carpeta con id 10 no existe o ha sido eliminada."
}
```

---

## Códigos de Error Comunes

| Código | Descripción | HTTP Status |
|--------|-------------|-------------|
| `CREDENCIALES_INVALIDAS` | Email o contraseña incorrectos | 401 |
| `SIN_ORGANIZACION` | Usuario sin organizaciones activas | 403 |
| `ORGANIZACION_CONFIG_INVALIDA` | Configuración de organización inválida | 409 |
| `ORGANIZACION_NO_ACCESIBLE` | Usuario no puede acceder a la organización | 403 |
| `ERROR_VALIDACION` | Validación de campos fallida | 400 |
| `SIN_PERMISOS` | Usuario sin permisos para la operación | 403 |
| `SIN_PERMISOS_ESCRITURA` | Sin permisos de escritura en carpeta | 403 |
| `CARPETA_NO_ENCONTRADA` | Carpeta no existe | 404 |
| `DOCUMENTO_NO_ENCONTRADO` | Documento no existe | 404 |
| `TOKEN_EXPIRADO` | Token JWT expirado | 401 |
| `TOKEN_INVALIDO` | Token JWT malformado o inválido | 401 |

---

## Autenticación

Todos los endpoints (excepto `/auth/login`) requieren autenticación mediante JWT Bearer Token.

### Formato del Header:

```http
Authorization: Bearer <token>
```

### Claims del JWT:

```json
{
  "sub": "usuario@example.com",
  "userId": 123,
  "organizacionId": 1,
  "roles": ["ADMIN", "USER"],
  "exp": 1703001234
}
```

### Expiración:

Los tokens tienen una validez de **1 hora (3600 segundos)** por defecto. Después de expirar, el cliente debe solicitar un nuevo token mediante `/auth/login` o implementar un mecanismo de refresh token (futuro).

---

## Rate Limiting (Planificado)

En producción, se aplicará rate limiting por IP y por usuario:

- **Anónimo (sin autenticar):** 10 requests/minuto
- **Autenticado:** 100 requests/minuto
- **Carga de archivos:** 10 uploads/hora por usuario

Las respuestas incluirán headers estándar:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1703001234
```

---

## Versionado de API (Futuro)

Actualmente la API está en versión v1 (implícita en las rutas). En futuras versiones se usará:

```
/api/v2/documentos
```

La versión v1 se mantendrá durante al menos 12 meses después de la introducción de v2.

---

Para información sobre arquitectura, ver [ARCHITECTURE.md](ARCHITECTURE.md).

Para detalles del modelo de datos, ver [DATABASE.md](DATABASE.md).

Para especificación OpenAPI completa, ver [ai-specs/specs/api-spec.yml](../ai-specs/specs/api-spec.yml).
