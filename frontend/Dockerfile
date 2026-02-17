# ========================================
# DocFlow - Frontend Application Dockerfile
# ========================================
# Multi-stage build para optimizar tamaño de imagen
# Etapa 1: Build con Node.js
# Etapa 2: Runtime con Nginx

# ----------------------------------------
# Stage 1: Build
# ----------------------------------------
FROM node:18.19.0-alpine3.19 AS builder

WORKDIR /app

# Copiar archivos de dependencias primero (mejor cache)
COPY package*.json ./

# Instalar dependencias
RUN npm ci --only=production=false

# Copiar código fuente
COPY . .

# Build de producción
# Asegurar permisos ejecutables en los binarios instalados (evita exit code 126)
RUN if [ -d node_modules/.bin ]; then chmod -R a+rx node_modules/.bin || true; fi
RUN npm run build

# ----------------------------------------
# Stage 2: Runtime
# ----------------------------------------
FROM nginx:1.27.0-alpine3.19

# Metadata
LABEL org.opencontainers.image.title="DocFlow Frontend"
LABEL org.opencontainers.image.description="Aplicación frontend SPA de DocFlow"
LABEL org.opencontainers.image.version="0.0.1"

# Copiar build desde stage anterior
COPY --from=builder /app/dist /usr/share/nginx/html

# Copiar configuración personalizada de nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exponer puerto (solo documentación)
EXPOSE 80

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Nginx se ejecuta como usuario no root por defecto en esta imagen
# El comando por defecto de la imagen base es suficiente
CMD ["nginx", "-g", "daemon off;"]
