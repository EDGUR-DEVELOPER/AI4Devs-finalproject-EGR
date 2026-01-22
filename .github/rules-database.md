# Reglas de diseño de base de datos

Estas reglas aplican a los esquemas de base de datos utilizados por los servicios backend (identidad, documentos, auditoría, etc.).

## 1. Alcance

- Bases de datos relacionales con PostgreSQL utilizadas por servicios como **identity** y **document-core**.
- Bases de datos NoSQL con MongoDB utilizadas por servicios como **auditLog**.

## 2. Convenciones de nombres

- Usar nombres descriptivos para esquemas, tablas y columnas.
- Mantener una convención consistente (por ejemplo `snake_case` en tablas y columnas) según defina el equipo.
- Evitar abreviaturas poco claras.

## 3. Relaciones e integridad

- Definir claves primarias claras y estables.
- Usar claves foráneas para representar relaciones entre entidades cuando aplique.
- Definir restricciones (NOT NULL, UNIQUE, CHECK) para garantizar la integridad de los datos.

## 4. Evolución del esquema

- Cualquier cambio en el esquema debe estar ligado a una historia de usuario o requerimiento claro.
- Mantener compatibilidad hacia atrás cuando sea posible, especialmente en entornos compartidos.
- Documentar en el Pull Request el impacto de los cambios de esquema.

## 5. Rendimiento e índices

- Crear índices para consultas frecuentes y claves foráneas según sea necesario.
- Revisar el impacto de nuevos índices en el rendimiento de escritura.

## 6. Referencias

- Índice general de reglas: [RULES.md](./RULES.md)
