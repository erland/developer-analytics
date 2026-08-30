# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /workspace/frontend

COPY frontend/package.json ./
RUN npm install --no-audit --no-fund

COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine AS runtime

COPY deploy/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
