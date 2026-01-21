# ========================================
# DocFlow - Document Core Service Dockerfile
# ========================================
# Multi-stage build para optimizar tamaño de imagen
# Etapa 1: Build con Maven
# Etapa 2: Runtime con JRE slim

# ----------------------------------------
# Stage 1: Build
# ----------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copiar archivos de configuración de Maven primero (mejor cache)
COPY pom.xml .

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Construir el JAR (skip tests para build más rápido)
RUN mvn clean package -DskipTests -B

# ----------------------------------------
# Stage 2: Runtime
# ----------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL org.opencontainers.image.title="DocFlow Document Core Service"
LABEL org.opencontainers.image.description="Servicio principal de gestión de documentos para DocFlow"
LABEL org.opencontainers.image.version="0.0.1-SNAPSHOT"

# Crear usuario no root para seguridad
RUN addgroup -S docflow && adduser -S docflow -G docflow

# Instalar curl para healthchecks
RUN apk add --no-cache curl

WORKDIR /app

# Copiar JAR desde stage de build
COPY --from=builder /build/target/*.jar app.jar

# Cambiar ownership
RUN chown -R docflow:docflow /app

# Cambiar a usuario no root
USER docflow

# Exponer puerto (solo documentación)
EXPOSE 8082

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SERVER_PORT=8082

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
