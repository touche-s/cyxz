# ============================================================
# Cyxz 前端 Dockerfile（Vite 构建 + Nginx 托管）
# ============================================================

# ===== Stage 1: Vite Build =====
FROM node:20-alpine AS builder
WORKDIR /build
COPY cyxz-frontend/package*.json ./
RUN npm ci
COPY cyxz-frontend/ .
RUN npm run build

# ===== Stage 2: Nginx =====
FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY docker/nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
