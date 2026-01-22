# Reglas de infraestructura y Docker

Estas reglas aplican a la infraestructura local orquestada con **Docker Compose**, tanto en la raíz del proyecto como en servicios específicos.

Fuente principal: documentación en `README-docker.md` y archivos `docker-compose.yml` del repositorio.

## 1. Alcance

- Archivo `docker-compose.yml` en la raíz del proyecto.
- Archivos `docker-compose.yml` específicos de servicios (por ejemplo en `backend/broker/`).

## 2. Nombres de servicios y puertos

- Mantener nombres de servicios descriptivos y consistentes.
- Respetar los puertos documentados en `README-docker.md` para evitar conflictos.
- Evitar cambios de puertos sin actualizar la documentación.

## 3. Variables de entorno y secretos

- Usar archivos `.env` (o variables de entorno) para credenciales y configuraciones sensibles.
- No commitear secretos ni tokens reales al repositorio.
- Documentar en `README-docker.md` las variables requeridas para levantar el entorno.

## 4. Volúmenes y datos

- Definir volúmenes nombrados para datos que deban persistir entre reinicios.
- Evitar perder datos críticos al recrear contenedores (salvo en entornos claramente marcados como de desarrollo).

## 5. Buenas prácticas de orquestación

- Mantener servicios relacionados en la misma red de Docker cuando sea necesario.
- Definir healthchecks básicos cuando aplique (BD, Redis, etc.).
- Probar localmente los cambios en `docker-compose.yml` antes de fusionar.

## 6. Referencias

- Índice general de reglas: [RULES.md](./RULES.md)
- Documentación de entorno local: [README-docker.md](../README-docker.md)
