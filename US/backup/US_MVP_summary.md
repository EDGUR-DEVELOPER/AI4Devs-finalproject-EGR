# Resumen de Historias: Enfoque MVP vs Post-MVP

Fecha: 2026-01-14

Resumen corto: Tras la decisión de retirar temporalmente Redis, MongoDB, Kafka, Audit Log y Vault para acelerar el MVP, las historias se han reclasificado. El objetivo es entregar un producto funcional mínimo y replanificar las capacidades asíncronas/infra en Post-MVP.

## Clasificación

- P5 — Auditoría (todas): **Post-MVP** (replanificadas)
  - US-AUDIT-001 .. US-AUDIT-005 → Post-MVP
  - Nota: Para trazabilidad mínima en MVP, se recomienda implementar una tabla `Evento_Auditoria` en PostgreSQL con insert síncrono desde puntos críticos (opcional, coste bajo).

- P6 — Búsqueda básica sin IA:
  - US-SEARCH-001: **MVP** — Búsqueda por texto implementada con índices en PostgreSQL.
  - US-SEARCH-002: **MVP (básico)** — Filtrado por permisos via joins/queries síncronas; optimizaciones avanzadas Post-MVP.
  - US-SEARCH-003: **MVP** — UI mínima de búsqueda consume API síncrona.

## Impacto por área

- Infraestructura: Se eliminan servicios Kafka, MongoDB, Redis, Vault del despliegue MVP. Quedan: PostgreSQL, MinIO, Gateway, Identity, Document Core, Frontend.
- Auditoría: Pasa a Post-MVP; alternativas MVP: registro simple en Postgres o logs estructurados.
- Búsqueda: Mantener funcionalidad básica en SQL; dejar IA/Indexación avanzada para Post-MVP.

## Tareas recomendadas (MVP)

1. Crear migración SQL para tabla `Evento_Auditoria` (si se requiere trazabilidad mínima).
2. Implementar puntos de emisión síncrona (inserts) en acciones críticas: login, crear carpeta, subir documento, asignación roles (opcional).
3. Asegurar que `SearchService` funciona con queries y índices en Postgres.

## Tareas recomendadas (Post-MVP)

1. Reinstalar Kafka + Broker + Audit Log (MongoDB) y mover emisión de eventos a pipeline asíncrono.
2. Implementar Indexer asíncrono (Search AI / Vector DB) para búsquedas semánticas y relevancia avanzada.
3. Integrar HashiCorp Vault para gestión centralizada de secretos.

---
Contacto: PM / Responsable técnico para dudas: equipo DocFlow