## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-004] Permiso recursivo en carpeta (herencia simple)

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Configurar un permiso de carpeta como recursivo (`recursivo=true`)
- Aplicar permisos heredados a subcarpetas automáticamente
- Diferenciar entre permisos recursivos y no recursivos

### Restricciones Implícitas
- La herencia es de padre a hijos (no inversa)
- El flag `recursivo` determina si aplica a subcarpetas
- La evaluación de herencia debe ser eficiente
- No se implementa herencia compleja (múltiples niveles de override)

### Riesgos o Ambigüedades
- No se especifica límite de profundidad de herencia
- **Suposición:** La herencia aplica a todos los niveles descendientes
- No se especifica qué pasa si hay conflicto padre-hijo
- **Suposición:** Para MVP, el permiso más específico (más cercano) prevalece
- El campo `recursivo` ya fue contemplado en US-ACL-002

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Agregar índice para consultas de herencia de carpetas
* **Objetivo:** Optimizar queries que buscan permisos en la jerarquía de carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Crear índice compuesto en `ACL_Carpeta` para campos (`usuario_id`, `recursivo`, `organizacion_id`). Asegurar que `Carpeta` tenga índice en `carpeta_padre_id` para navegación eficiente.
* **Entregables:**
    - Migración con índice para queries de herencia.
    - Índice en `Carpeta(carpeta_padre_id)` si no existe.
    - Análisis de plan de ejecución de queries.

---

* **Título:** Query para obtener ruta de ancestros de carpeta
* **Objetivo:** Facilitar evaluación de herencia hacia arriba.
* **Tipo:** Tarea
* **Descripción corta:** Implementar stored procedure o CTE recursivo que dado un `carpeta_id`, devuelva la lista ordenada de ancestros (de hijo a raíz). Útil para evaluar permisos heredados.
* **Entregables:**
    - Query/función `getAncestorPath(carpeta_id)`.
    - Documentación de uso y performance.

---
### Backend
---

* **Título:** Servicio de resolución de ruta de ancestros
* **Objetivo:** Obtener la jerarquía de carpetas para evaluar herencia.
* **Tipo:** Tarea
* **Descripción corta:** Implementar método que dado un `carpeta_id`, retorne la lista de IDs de carpetas ancestras en orden (de más cercano a raíz). Usar caché si la jerarquía es estable.
* **Entregables:**
    - Método `obtenerRutaAncestros(carpetaId)` en `CarpetaService`.
    - Lista ordenada de IDs ancestros.
    - Cache opcional por carpeta.

---

* **Título:** Algoritmo de evaluación de permiso heredado
* **Objetivo:** Determinar si un usuario tiene permiso sobre carpeta por herencia.
* **Tipo:** Tarea
* **Descripción corta:** Implementar algoritmo que: 1) Busque ACL directo en carpeta, 2) Si no existe, recorra ancestros buscando ACL con `recursivo=true`. Retornar el primer permiso encontrado o null.
* **Entregables:**
    - Método `resolverPermisoHeredado(usuarioId, carpetaId)`.
    - Lógica de recorrido de ancestros.
    - Retorno de `NivelAcceso` o null.

---

* **Título:** Integrar herencia en servicio de evaluación de permisos
* **Objetivo:** Unificar evaluación de permisos directos y heredados.
* **Tipo:** Tarea
* **Descripción corta:** Modificar el servicio principal de permisos para que primero busque permiso directo, y si no existe, invoque la resolución de herencia. Este método será usado por guards y endpoints.
* **Entregables:**
    - Método `evaluarPermiso(usuarioId, carpetaId)` integrado.
    - Lógica: directo > heredado > sin permiso.

---

* **Título:** Endpoint para verificar permiso efectivo sobre carpeta
* **Objetivo:** Permitir consultar qué permiso tiene un usuario sobre una carpeta.
* **Tipo:** Historia
* **Descripción corta:** Endpoint `GET /carpetas/{id}/mi-permiso` que devuelve el nivel de acceso efectivo del usuario autenticado, indicando si es directo o heredado.
* **Entregables:**
    - Ruta/controlador `GET /carpetas/{id}/mi-permiso`.
    - Respuesta: `{ nivel_acceso, es_heredado, carpeta_origen_id }`.
    - Documentación OpenAPI.

---

* **Título:** Pruebas unitarias de algoritmo de herencia
* **Objetivo:** Asegurar correcta evaluación de permisos heredados.
* **Tipo:** QA
* **Descripción corta:** Tests que cubran: permiso directo existe (no hereda), permiso recursivo en padre aplica, permiso no recursivo en padre no aplica, herencia en múltiples niveles.
* **Entregables:**
    - Suite de tests para `resolverPermisoHeredado()`.
    - Casos: directo, recursivo, no-recursivo, multinivel.

---

* **Título:** Pruebas de integración de herencia de permisos
* **Objetivo:** Verificar herencia en escenarios reales.
* **Tipo:** QA
* **Descripción corta:** Tests E2E con jerarquía de carpetas (abuelo/padre/hijo). Verificar que usuario con permiso recursivo en abuelo puede acceder a nieto, y sin recursivo no puede.
* **Entregables:**
    - Tests con estructura de carpetas multinivel.
    - Verificación de scenario 1 y 2 de la historia.

---
### Frontend
---

* **Título:** Mostrar origen de permiso en UI
* **Objetivo:** Informar al usuario de dónde viene su permiso.
* **Tipo:** Tarea
* **Descripción corta:** En la vista de carpeta, mostrar badge o tooltip indicando si el permiso es "directo" o "heredado de [carpeta padre]". Usar el endpoint de permiso efectivo.
* **Entregables:**
    - Indicador visual de origen de permiso.
    - Tooltip con nombre de carpeta origen si es heredado.

---

* **Título:** Checkbox "Aplicar a subcarpetas" en formulario de permisos
* **Objetivo:** Permitir configurar recursividad al asignar permisos.
* **Tipo:** Tarea
* **Descripción corta:** En el modal de asignar permiso (US-ACL-002), agregar checkbox "Aplicar a subcarpetas (recursivo)" con valor por defecto según contexto. Incluir tooltip explicativo.
* **Entregables:**
    - Checkbox en formulario de asignación.
    - Tooltip explicando el comportamiento.
    - Valor enviado en request.

---

* **Título:** Visualizar permisos heredados en lista de permisos
* **Objetivo:** Distinguir permisos directos de heredados en la administración.
* **Tipo:** Tarea
* **Descripción corta:** En la lista de permisos de carpeta (US-ACL-003), mostrar tanto permisos directos como heredados, diferenciándolos visualmente. Los heredados no deben tener opción de revocar directo.
* **Entregables:**
    - Lista mostrando permisos directos y heredados.
    - Estilo diferenciado (icono/color/badge).
    - Opción "Ir a carpeta origen" para heredados.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BD] Agregar índice para consultas de herencia
   ↓
2. [BD] Query para obtener ruta de ancestros
   ↓
3. [BE] Servicio de resolución de ruta de ancestros
   ↓
4. [BE] Algoritmo de evaluación de permiso heredado
   ↓
5. [BE] Integrar herencia en servicio de evaluación
   ↓
6. [BE] Endpoint GET /carpetas/{id}/mi-permiso
   ↓
7. [QA] Pruebas unitarias + integración
   ↓
8. [FE] Checkbox "Aplicar a subcarpetas"
   ↓
9. [FE] Mostrar origen de permiso en UI
   ↓
10. [FE] Visualizar permisos heredados en lista
```

### Dependencias entre Tickets
- Depende de US-ACL-002 (tabla ACL con campo `recursivo`)
- Depende de P3 (tabla Carpeta con `carpeta_padre_id`)
- El algoritmo de herencia es usado por US-ACL-006, US-ACL-007, US-ACL-008

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Algoritmo de evaluación de permiso heredado (lógica compleja, crítica)
- Query de ruta de ancestros (performance importante)

### Tickets para Escenarios BDD
```gherkin
Feature: Herencia de permisos en subcarpetas
  
  Background:
    Given existe jerarquía: "Raiz" > "Padre" > "Hijo" > "Nieto"
    And usuario "juan@test.com" del organizacion "A"

  Scenario: Permiso recursivo aplica a subcarpetas
    Given permiso "LECTURA" con recursivo=true para Juan en "Padre"
    When Juan accede a carpeta "Nieto"
    Then tiene permiso "LECTURA"
    And el permiso es heredado de "Padre"

  Scenario: Permiso no recursivo no aplica a subcarpetas
    Given permiso "LECTURA" con recursivo=false para Juan en "Padre"
    When Juan accede a carpeta "Hijo"
    Then no tiene permiso
    And recibe status 403

  Scenario: Permiso directo tiene prioridad sobre heredado
    Given permiso "LECTURA" con recursivo=true para Juan en "Raiz"
    And permiso "ESCRITURA" directo para Juan en "Hijo"
    When Juan consulta su permiso en "Hijo"
    Then tiene permiso "ESCRITURA"
    And el permiso es directo
```

---

## [Enhanced] Especificación Técnica Detallada

### Narrativa Enriquecida

**Como** administrador de carpetas con permisos de `ADMINISTRACION`  
**Quiero** configurar un permiso de carpeta con el flag `recursivo=true` para que se aplique automáticamente a todas las subcarpetas descendientes  
**Para** evitar la configuración repetitiva de permisos en estructuras jerárquicas profundas y mantener consistencia en el control de acceso.

---

### Criterios de Aceptación Detallados

#### Scenario 1: Permiso recursivo aplica a todos los niveles descendientes

**Dado que:**
- Existe una jerarquía de carpetas: `Raíz (ID: 1)` → `Proyectos (ID: 2)` → `2024 (ID: 3)` → `Q1 (ID: 4)`
- Todas las carpetas pertenecen a la organización con `organizacion_id = 10`
- Existe el usuario `ana.garcia@example.com` (ID: 50) perteneciente a la organización 10
- Se crea un ACL en la carpeta "Proyectos" (ID: 2):
  ```json
  {
    "usuario_id": 50,
    "carpeta_id": 2,
    "nivel_acceso_codigo": "LECTURA",
    "recursivo": true,
    "organizacion_id": 10
  }
  ```

**Cuando:**
1. Ana intenta acceder a la carpeta "Q1" (ID: 4) mediante `GET /api/carpetas/4`
2. El sistema ejecuta el algoritmo de evaluación de permisos
3. No encuentra ACL directo para Ana en carpeta 4
4. Recorre la ruta de ancestros: `[3, 2, 1]`
5. Encuentra ACL con `recursivo=true` en carpeta 2

**Entonces:**
- El sistema retorna `200 OK`
- Ana puede listar el contenido de la carpeta "Q1"
- El endpoint `GET /api/carpetas/4/mi-permiso` retorna:
  ```json
  {
    "nivel_acceso": "LECTURA",
    "es_heredado": true,
    "carpeta_origen_id": 2,
    "carpeta_origen_nombre": "Proyectos",
    "ruta_herencia": ["Proyectos", "2024", "Q1"]
  }
  ```
- Se registra un evento de auditoría:
  ```
  codigo_evento: "CARPETA_ACCESO_HEREDADO"
  usuario_id: 50
  carpeta_id: 4
  carpeta_origen_acl_id: 2
  nivel_acceso: "LECTURA"
  ```

**Validaciones:**
- El `organizacion_id` del token JWT debe coincidir con el de la carpeta y el ACL
- La carpeta no debe tener `fecha_eliminacion` (soft delete)
- El usuario debe estar activo (`activo=true`)

---

#### Scenario 2: Permiso no recursivo NO aplica a subcarpetas

**Dado que:**
- Existe una jerarquía: `Documentos (ID: 100)` → `Finanzas (ID: 101)`
- Usuario `carlos.lopez@example.com` (ID: 51, organizacion_id: 10)
- ACL en carpeta "Documentos" (ID: 100):
  ```json
  {
    "usuario_id": 51,
    "carpeta_id": 100,
    "nivel_acceso_codigo": "ESCRITURA",
    "recursivo": false,
    "organizacion_id": 10
  }
  ```

**Cuando:**
1. Carlos intenta acceder a carpeta "Finanzas" (ID: 101) mediante `GET /api/carpetas/101`
2. El sistema no encuentra ACL directo en carpeta 101
3. Recorre ancestros y encuentra ACL en carpeta 100
4. El ACL tiene `recursivo=false`

**Entonces:**
- El sistema retorna `403 Forbidden`
- Mensaje de error:
  ```json
  {
    "error": {
      "codigo": "PERMISO_DENEGADO",
      "mensaje": "No tienes permiso para acceder a esta carpeta",
      "detalle": "No se encontró permiso directo ni heredado"
    }
  }
  ```
- NO se registra evento de acceso, solo intento fallido:
  ```
  codigo_evento: "CARPETA_ACCESO_DENEGADO"
  usuario_id: 51
  carpeta_id: 101
  razon: "SIN_PERMISO_HEREDADO"
  ```

**Validaciones:**
- El algoritmo debe detenerse al encontrar el primer ACL no recursivo en la cadena de ancestros
- No debe continuar buscando en ancestros superiores

---

#### Scenario 3: Permiso directo tiene precedencia sobre heredado

**Dado que:**
- Jerarquía: `Root (ID: 1)` → `Legal (ID: 2)` → `Contratos (ID: 3)`
- Usuario `maria.sanchez@example.com` (ID: 52, organizacion_id: 10)
- ACL recursivo en "Root" (ID: 1):
  ```json
  {
    "usuario_id": 52,
    "carpeta_id": 1,
    "nivel_acceso_codigo": "LECTURA",
    "recursivo": true
  }
  ```
- ACL directo en "Contratos" (ID: 3):
  ```json
  {
    "usuario_id": 52,
    "carpeta_id": 3,
    "nivel_acceso_codigo": "ADMINISTRACION",
    "recursivo": false
  }
  ```

**Cuando:**
1. María accede a carpeta "Contratos" (ID: 3)
2. El algoritmo encuentra primero el ACL directo

**Entonces:**
- El sistema retorna `200 OK`
- El endpoint `/api/carpetas/3/mi-permiso` retorna:
  ```json
  {
    "nivel_acceso": "ADMINISTRACION",
    "es_heredado": false,
    "carpeta_origen_id": 3,
    "ruta_herencia": null
  }
  ```
- María tiene capacidades de administración (gestionar permisos, eliminar, etc.)
- El permiso heredado de "Root" es ignorado

**Regla de precedencia:**
1. **Permiso directo** (si existe) → usar siempre
2. **Permiso heredado** más cercano con `recursivo=true` → usar si no hay directo
3. **Sin permiso** → denegar acceso (403)

---

### Algoritmo de Evaluación de Herencia

```typescript
function evaluarPermisoEfectivo(
  usuarioId: number,
  carpetaId: number,
  organizacionId: number
): PermisoEfectivo | null {
  
  // 1. Buscar ACL directo
  const aclDirecto = buscarACL(usuarioId, carpetaId, organizacionId);
  if (aclDirecto) {
    return {
      nivelAcceso: aclDirecto.nivel_acceso_codigo,
      esHeredado: false,
      carpetaOrigenId: carpetaId
    };
  }
  
  // 2. Obtener ruta de ancestros (de más cercano a raíz)
  const rutaAncestros = obtenerRutaAncestros(carpetaId);
  
  // 3. Recorrer ancestros buscando ACL recursivo
  for (const ancestroId of rutaAncestros) {
    const aclAncestro = buscarACL(usuarioId, ancestroId, organizacionId);
    
    if (aclAncestro && aclAncestro.recursivo) {
      return {
        nivelAcceso: aclAncestro.nivel_acceso_codigo,
        esHeredado: true,
        carpetaOrigenId: ancestroId,
        rutaHerencia: construirRutaHerencia(ancestroId, carpetaId)
      };
    }
    
    // Si encontramos ACL no recursivo, detenemos búsqueda
    if (aclAncestro && !aclAncestro.recursivo) {
      break;
    }
  }
  
  // 4. No se encontró permiso
  return null;
}
```

---

### Estructura de Datos

#### Tabla ACL_Carpeta (existente, con índices adicionales)

```sql
CREATE TABLE ACL_Carpeta (
  id SERIAL PRIMARY KEY,
  usuario_id INT NOT NULL,
  carpeta_id INT NOT NULL,
  nivel_acceso_codigo VARCHAR(50) NOT NULL,
  recursivo BOOLEAN NOT NULL DEFAULT false,
  organizacion_id INT NOT NULL,
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP,
  creado_por INT,
  
  CONSTRAINT fk_acl_carpeta_usuario FOREIGN KEY (usuario_id) 
    REFERENCES Usuario(id),
  CONSTRAINT fk_acl_carpeta_carpeta FOREIGN KEY (carpeta_id) 
    REFERENCES Carpeta(id),
  CONSTRAINT fk_acl_carpeta_organizacion FOREIGN KEY (organizacion_id) 
    REFERENCES Organizacion(id),
  CONSTRAINT uk_acl_carpeta UNIQUE (usuario_id, carpeta_id, organizacion_id)
);

-- Índice para consultas de herencia (nuevo)
CREATE INDEX idx_acl_carpeta_herencia 
ON ACL_Carpeta(usuario_id, recursivo, organizacion_id) 
WHERE recursivo = true;

-- Índice para navegación por carpeta
CREATE INDEX idx_acl_carpeta_carpeta_usuario 
ON ACL_Carpeta(carpeta_id, usuario_id, organizacion_id);
```

#### Tabla Carpeta (con índice para ancestros)

```sql
CREATE INDEX idx_carpeta_padre 
ON Carpeta(carpeta_padre_id, organizacion_id) 
WHERE fecha_eliminacion IS NULL;
```

#### Query CTE Recursivo para Ancestros

```sql
WITH RECURSIVE ancestros AS (
  -- Caso base: carpeta actual
  SELECT id, carpeta_padre_id, nombre, 0 AS nivel
  FROM Carpeta
  WHERE id = :carpeta_id
    AND organizacion_id = :organizacion_id
    AND fecha_eliminacion IS NULL
  
  UNION ALL
  
  -- Caso recursivo: padres
  SELECT c.id, c.carpeta_padre_id, c.nombre, a.nivel + 1
  FROM Carpeta c
  INNER JOIN ancestros a ON c.id = a.carpeta_padre_id
  WHERE c.organizacion_id = :organizacion_id
    AND c.fecha_eliminacion IS NULL
)
SELECT id, nombre, nivel
FROM ancestros
WHERE nivel > 0  -- Excluir la carpeta misma
ORDER BY nivel ASC;  -- Más cercano primero
```

---

### Endpoints de API

#### GET /api/carpetas/{carpetaId}/mi-permiso

Obtiene el permiso efectivo del usuario autenticado sobre una carpeta.

**Headers Requeridos:**
```
Authorization: Bearer <JWT>
X-Organization-Id: <organizacion_id>
```

**Response 200 OK:**
```json
{
  "data": {
    "carpeta_id": 4,
    "carpeta_nombre": "Q1",
    "nivel_acceso": "LECTURA",
    "es_heredado": true,
    "carpeta_origen": {
      "id": 2,
      "nombre": "Proyectos",
      "ruta": "/Raíz/Proyectos"
    },
    "ruta_herencia": ["Proyectos", "2024", "Q1"],
    "acciones_permitidas": [
      "ver", "listar", "descargar"
    ]
  }
}
```

**Response 403 Forbidden:**
```json
{
  "error": {
    "codigo": "PERMISO_DENEGADO",
    "mensaje": "No tienes permiso para acceder a esta carpeta",
    "carpeta_id": 101,
    "organizacion_id": 10
  }
}
```

**Response 404 Not Found:**
```json
{
  "error": {
    "codigo": "CARPETA_NO_ENCONTRADA",
    "mensaje": "La carpeta no existe o fue eliminada"
  }
}
```

#### Modificación en POST /api/carpetas/{carpetaId}/permisos

Agregar checkbox `recursivo` al crear ACL (ya implementado en US-ACL-002, ahora se enfatiza su uso):

**Request Body:**
```json
{
  "usuario_id": 50,
  "nivel_acceso_codigo": "LECTURA",
  "recursivo": true,
  "comentario_opcional": "Acceso a todos los proyectos de 2024"
}
```

**Validaciones adicionales:**
- Si `recursivo=true`, verificar que la carpeta tiene o puede tener subcarpetas
- Solo usuarios con `ADMINISTRACION` pueden crear ACLs recursivos (opcional para MVP)

---

### Casos Edge y Manejo de Errores

#### Edge Case 1: Ciclos en jerarquía (no deberían existir)

**Prevención:**
- La tabla `Carpeta` debe tener constraint que evite `carpeta_padre_id = id`
- El algoritmo debe limitar la profundidad máxima de búsqueda (ej. 50 niveles)
- Si se detecta ciclo (ancestro ya visitado), lanzar `InternalServerError`

```typescript
const MAX_DEPTH = 50;
let visitados = new Set<number>();

for (const ancestroId of rutaAncestros) {
  if (visitados.has(ancestroId)) {
    throw new Error("Ciclo detectado en jerarquía de carpetas");
  }
  if (visitados.size >= MAX_DEPTH) {
    throw new Error("Profundidad máxima excedida");
  }
  visitados.add(ancestroId);
  // ... continuar evaluación
}
```

#### Edge Case 2: Carpeta raíz sin padre

**Manejo:**
- La carpeta raíz tiene `carpeta_padre_id = NULL`
- El query CTE debe manejar correctamente este caso
- La evaluación de herencia termina al llegar a la raíz

#### Edge Case 3: Múltiples ACLs recursivos en la cadena

**Comportamiento:**
- Se usa el **más cercano** (primer ancestro con ACL recursivo)
- Ancestros superiores son ignorados
- Esto permite "override" parcial de permisos heredados

**Ejemplo:**
- Raíz (ID: 1): ACL recursivo `LECTURA`
- Proyectos (ID: 2): ACL recursivo `ESCRITURA`
- 2024 (ID: 3): Sin ACL directo

Usuario accediendo a "2024" → recibe `ESCRITURA` (de Proyectos), no `LECTURA` (de Raíz).

#### Edge Case 4: Cambio de recursividad en ACL existente

**Comportamiento:**
- Al actualizar un ACL de `recursivo=false` a `true`:
  - Los permisos se aplican inmediatamente a todos los descendientes
  - No requiere recálculo de caché (evaluación en tiempo real)
- Al cambiar de `true` a `false`:
  - Los descendientes pierden el permiso heredado inmediatamente
  - Pueden necesitar ACLs directos si tenían dependencia

**Auditoría:**
```
codigo_evento: "ACL_RECURSIVIDAD_MODIFICADA"
acl_id: 123
recursivo_anterior: false
recursivo_nuevo: true
afecta_descendientes: true
```

---

### Optimización y Performance

#### Cache de Rutas de Ancestros

```typescript
class CarpetaService {
  private cacheAncestros = new Map<number, number[]>();
  
  obtenerRutaAncestros(carpetaId: number): number[] {
    if (this.cacheAncestros.has(carpetaId)) {
      return this.cacheAncestros.get(carpetaId)!;
    }
    
    const ruta = this.queryAncestros(carpetaId);
    
    // Cache por 1 hora (la jerarquía es relativamente estable)
    this.cacheAncestros.set(carpetaId, ruta);
    setTimeout(() => this.cacheAncestros.delete(carpetaId), 3600000);
    
    return ruta;
  }
  
  invalidarCacheAncestros(carpetaId: number): void {
    // Llamar cuando se mueve/elimina una carpeta
    this.cacheAncestros.delete(carpetaId);
  }
}
```

#### Índices de Base de Datos

**Plan de ejecución esperado:**
1. Buscar ACL directo: `O(1)` con índice `idx_acl_carpeta_carpeta_usuario`
2. Obtener ancestros: `O(log n)` con índice `idx_carpeta_padre` y CTE
3. Buscar ACL en cada ancestro: `O(1)` por ancestro con índices

**Medición de performance:**
- Evaluación de permiso: < 10ms para jerarquías de hasta 20 niveles
- Query CTE: < 5ms
- Con cache: < 1ms

---

### Seguridad

#### Aislamiento Multi-Tenant

```typescript
// Verificar organizacion_id en todos los queries
function buscarACL(usuarioId: number, carpetaId: number, orgId: number) {
  return db.query(`
    SELECT * FROM ACL_Carpeta
    WHERE usuario_id = $1
      AND carpeta_id = $2
      AND organizacion_id = $3  -- CRÍTICO: aislamiento
  `, [usuarioId, carpetaId, orgId]);
}

// El organizacion_id SIEMPRE viene del token JWT, nunca del request body
const organizacionId = req.user.organizacion_id; // Del token
```

#### Prevención de Escalada de Privilegios

**Validaciones:**
1. Usuario solo puede ver permisos de carpetas a las que ya tiene acceso (directo o heredado)
2. Solo usuarios con `ADMINISTRACION` pueden ver todos los ACLs de una carpeta
3. Endpoint `/mi-permiso` solo retorna permiso del usuario autenticado (no permite `?usuario_id=otro`)

#### Auditoría de Accesos Heredados

```typescript
interface EventoAuditoriaHerencia {
  codigo_evento: 'CARPETA_ACCESO_HEREDADO';
  usuario_id: number;
  carpeta_id: number;
  carpeta_origen_acl_id: number;
  nivel_acceso: string;
  ruta_herencia: string[];  // Array de IDs de carpetas
  timestamp: Date;
  organizacion_id: number;
}
```

---

### Testing

#### Tests Unitarios (TDD)

```typescript
describe('PermisoService.resolverPermisoHeredado', () => {
  it('debe retornar null cuando no existe ACL directo ni heredado', async () => {
    const permiso = await service.resolverPermisoHeredado(999, 999, 10);
    expect(permiso).toBeNull();
  });
  
  it('debe usar ACL directo ignorando herencia', async () => {
    // Setup: ACL directo ESCRITURA, heredado LECTURA
    const permiso = await service.resolverPermisoHeredado(50, 3, 10);
    expect(permiso.nivelAcceso).toBe('ESCRITURA');
    expect(permiso.esHeredado).toBe(false);
  });
  
  it('debe heredar de ancestro más cercano con recursivo=true', async () => {
    // Setup: Padre(LECTURA, recursivo=true), Abuelo(ESCRITURA, recursivo=true)
    const permiso = await service.resolverPermisoHeredado(50, 4, 10);
    expect(permiso.nivelAcceso).toBe('LECTURA');
    expect(permiso.carpetaOrigenId).toBe(2); // Padre, no Abuelo
  });
  
  it('debe ignorar ACL no recursivo en la cadena', async () => {
    // Setup: Padre(ESCRITURA, recursivo=false)
    const permiso = await service.resolverPermisoHeredado(50, 4, 10);
    expect(permiso).toBeNull();
  });
  
  it('debe respetar aislamiento de organización', async () => {
    // Setup: ACL de otra organización
    const permiso = await service.resolverPermisoHeredado(50, 3, 999);
    expect(permiso).toBeNull();
  });
});
```

#### Tests de Integración (E2E)

```gherkin
Feature: Herencia de permisos en subcarpetas
  
  Background:
    Given existe organización "TestOrg" con id 10
    And existe jerarquía de carpetas:
      | id  | nombre      | padre_id | organizacion_id |
      | 1   | Raíz        | null     | 10              |
      | 2   | Proyectos   | 1        | 10              |
      | 3   | 2024        | 2        | 10              |
      | 4   | Q1          | 3        | 10              |
    And existe usuario "ana@test.com" con id 50 en organización 10

  Scenario: Acceso exitoso con permiso recursivo
    Given existe ACL en carpeta 2:
      | usuario_id | nivel_acceso | recursivo |
      | 50         | LECTURA      | true      |
    When Ana (id:50) accede a GET /api/carpetas/4 con token válido
    Then la respuesta es 200 OK
    And puede listar el contenido de la carpeta
    
  Scenario: Acceso denegado con permiso no recursivo
    Given existe ACL en carpeta 2:
      | usuario_id | nivel_acceso | recursivo |
      | 50         | LECTURA      | false     |
    When Ana accede a GET /api/carpetas/4
    Then la respuesta es 403 Forbidden
    And el mensaje indica "No tienes permiso para acceder a esta carpeta"
    
  Scenario: Consultar permiso efectivo heredado
    Given existe ACL recursivo en carpeta 2 para Ana
    When Ana accede a GET /api/carpetas/4/mi-permiso
    Then la respuesta es 200 OK
    And el JSON contiene:
      | campo                  | valor       |
      | nivel_acceso           | LECTURA     |
      | es_heredado            | true        |
      | carpeta_origen.id      | 2           |
      | carpeta_origen.nombre  | Proyectos   |
```

---

### Documentación

#### Actualizar OpenAPI Spec

```yaml
/carpetas/{carpetaId}/mi-permiso:
  get:
    summary: Obtener permiso efectivo del usuario autenticado
    tags: [ACL, Carpetas]
    security:
      - BearerAuth: []
    parameters:
      - name: carpetaId
        in: path
        required: true
        schema:
          type: integer
    responses:
      '200':
        description: Permiso efectivo (directo o heredado)
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PermisoEfectivo'
      '403':
        $ref: '#/components/responses/Forbidden'
      '404':
        $ref: '#/components/responses/NotFound'

components:
  schemas:
    PermisoEfectivo:
      type: object
      required:
        - carpeta_id
        - nivel_acceso
        - es_heredado
      properties:
        carpeta_id:
          type: integer
        carpeta_nombre:
          type: string
        nivel_acceso:
          type: string
          enum: [LECTURA, ESCRITURA, ADMINISTRACION]
        es_heredado:
          type: boolean
        carpeta_origen:
          type: object
          nullable: true
          properties:
            id:
              type: integer
            nombre:
              type: string
            ruta:
              type: string
        ruta_herencia:
          type: array
          items:
            type: string
          nullable: true
        acciones_permitidas:
          type: array
          items:
            type: string
```

#### Actualizar README Backend

Agregar sección en `backend/document-core/README.md`:

```markdown
## Herencia de Permisos (US-ACL-004)

### Concepto

Los permisos de carpeta pueden configurarse como **recursivos** (`recursivo=true`), aplicándose automáticamente a todas las subcarpetas descendientes.

### Reglas de Evaluación

1. **Permiso Directo** → Se usa siempre si existe
2. **Permiso Heredado** → Se busca en ancestros si no hay directo
3. **Sin Permiso** → Acceso denegado (403)

### Ejemplo de Uso

```bash
# Crear permiso recursivo
POST /api/carpetas/2/permisos
{
  "usuario_id": 50,
  "nivel_acceso_codigo": "LECTURA",
  "recursivo": true
}

# Verificar permiso efectivo en subcarpeta
GET /api/carpetas/4/mi-permiso
```

### Performance

- Evaluación: < 10ms para jerarquías de 20 niveles
- Cache de ancestros: 1 hora
- Índices optimizados para consultas de herencia
```

---

### Criterios de Completitud

La historia US-ACL-004 se considera completa cuando:

- [ ] Índices de BD creados y plan de ejecución validado (< 10ms)
- [ ] Query CTE de ancestros implementado y testeado
- [ ] Algoritmo de evaluación de herencia implementado con tests unitarios (100% cobertura)
- [ ] Endpoint `GET /api/carpetas/{id}/mi-permiso` funcional
- [ ] Tests de integración E2E pasando (scenarios 1, 2 y 3)
- [ ] UI con checkbox "Aplicar a subcarpetas" funcionando
- [ ] UI mostrando origen de permiso (badge/tooltip)
- [ ] UI listando permisos directos y heredados diferenciados
- [ ] Documentación OpenAPI actualizada
- [ ] README backend con sección de herencia
- [ ] Tests de performance validando < 10ms
- [ ] Eventos de auditoría registrándose correctamente

---

### Dependencias

**Depende de:**
- US-ACL-002: Tabla `ACL_Carpeta` con campo `recursivo`
- US-FOLDER-001: Tabla `Carpeta` con `carpeta_padre_id`
- US-AUTH-004: Aislamiento por `organizacion_id`

**Bloquea a:**
- US-ACL-006: Precedencia de permisos (documento > carpeta > herencia)
- US-ACL-007: Enforcement de lectura con herencia
- US-ACL-008: Enforcement de escritura con herencia

---

### Notas Técnicas Adicionales

#### Consideraciones para Futuras Iteraciones

1. **Herencia Compleja (post-MVP):**
   - Override selectivo de permisos en puntos intermedios
   - Denegación explícita que bloquea herencia
   - Múltiples fuentes de herencia (grupos + usuarios)

2. **Cache Avanzado:**
   - Redis para cache distribuido de permisos evaluados
   - Invalidación inteligente cuando cambia la jerarquía
   - Pre-cálculo de permisos para usuarios frecuentes

3. **Métricas:**
   - Tiempo promedio de evaluación de herencia
   - Profundidad promedio de jerarquías
   - Hit rate de cache de ancestros
   - Distribución de permisos recursivos vs no recursivos

4. **Límites:**
   - Profundidad máxima de jerarquía: 50 niveles
   - Timeout de evaluación: 100ms
   - Cache TTL: 1 hora (configurable)

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | Agregar índice para consultas de herencia |
| 2 | BD | Query para obtener ruta de ancestros |
| 3 | BE | Servicio de resolución de ruta de ancestros |
| 4 | BE | Algoritmo de evaluación de permiso heredado |
| 5 | BE | Integrar herencia en servicio de evaluación |
| 6 | BE | Endpoint GET /carpetas/{id}/mi-permiso |
| 7 | QA | Pruebas unitarias de algoritmo de herencia |
| 8 | QA | Pruebas de integración de herencia |
| 9 | FE | Checkbox "Aplicar a subcarpetas" |
| 10 | FE | Mostrar origen de permiso en UI |
| 11 | FE | Visualizar permisos heredados en lista |
