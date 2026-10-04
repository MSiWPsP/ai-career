# syntax=docker/dockerfile:1.7

FROM node:22-alpine3.23 AS build

WORKDIR /workspace
ENV NODE_ENV=production \
    NODE_OPTIONS=--max-old-space-size=384 \
    VITE_API_BASE_URL=/api

COPY ai-career-web/package.json ai-career-web/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --include=dev

COPY ai-career-web/ ./
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.31.3-alpine3.24 AS runtime

USER root
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo Asia/Shanghai > /etc/timezone

# 显式设置只读权限，避免受服务器严格 umask / 源码上传权限影响，导致非 root Nginx 无法读取配置。
COPY --chmod=0644 ai-career-web/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build --chown=101:101 /workspace/dist/ /usr/share/nginx/html/

USER 101:101

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
